# NotificationBus是针对Android优化的发布-订阅事件总线，简化了Android组件间的通信。
# 1.概述
看过我之前关于EventBus 讲解的文章[《EventBus原理与源码解析》](https://www.jianshu.com/p/6da5abfec1da)，可以了解到EventBus是针对Android优化的发布-订阅事件总线，简化了Android组件间的通信。有了EventBus已经很方便我们平时日常开发中的组件通信了，但在仔细研读其源码之后，我也在之前的文章中提到了两个疑问：
1. 其消息给人感觉是一种乱跳的感觉，因为其采用注解的方式，这点感觉对业务逻辑梳理并不一定占有优势，就拿Android Studio来说，居然会提示该方法无处使用。
2. 采用反射方法invokeSubscriber来消费事件，效率如何。
基于以上的两个疑问，结合我目前做的项目，大概简单写了个通EventBus 具备相同功能框架 [NotificationCenter](https://github.com/devzhan/NotificationBus)。以下是关于NotificationBus的一些基本讲解。
# 2.基本使用
 [NotificationCenter](https://github.com/devzhan/NotificationBus)的基本使用和EventBus使用相差无几，均需要注册，发布，反注册这几个步骤。其基本流程图如下：
![NotificationCenter.png](https://upload-images.jianshu.io/upload_images/1594504-db54bdf42c3f328b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
## 2.1基本结构
从下图我们可以看出整个框架代码结构非常精简，同EventBus一样也是基于观察者模式来设计的。
![NotificationCenter—Structure.png](https://upload-images.jianshu.io/upload_images/1594504-29e3658cfd095492.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 2.2 使用方法
1. 项目引用NotificationCenter,在项目的build文件中添加
```    compile project(path: ':notificatonlibrary')```
2. 注册：
```
 @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationCenter.defaultCenter().subscriber(TOP_KEY,subscriber);
        NotificationCenter.defaultCenter().subscriber(EventSubscriber.class,eventSubscriber);
        initView();
    }
```
3. 反注册：
```
@Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.defaultCenter().unsubscribe(TOP_KEY,subscriber);
        NotificationCenter.defaultCenter().unsubscribe(EventSubscriber.class,eventSubscriber);
    }
```
4. 发布消息
```
 public void onClick(View v) {

        int viewId = v.getId();
        if (viewId==R.id.bt_send){
            NotificationCenter.defaultCenter().publish("top_key",null);
            EventSubscriber eventSubscriber = new EventSubscriber();
            NotificationCenter.defaultCenter().publish(eventSubscriber);
        }
    }
```
可以看到我们输出的log如下
![log.png](https://upload-images.jianshu.io/upload_images/1594504-133cdcd50452cecf.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

基本使用就是这么简单，具体可以参照[Demo](https://github.com/devzhan/NotificationBus)。

# 3. 源码解读
## 3.1注册，从上述使用的过程我们可以看到，存在两种注册方式
 ```      NotificationCenter.defaultCenter().subscriber(TOP_KEY,subscriber);```和```        NotificationCenter.defaultCenter().subscriber(EventSubscriber.class,eventSubscriber);```两种方式。首先先看defaultCenter 这个方法
```
public static NotificationCenter defaultCenter() {
        if (_instanceCenter == null) {
            _instanceCenter = new NotificationCenter();
        }
        return _instanceCenter ;
    }
```
也是一个单例模式，我们来看看构造方法NotificationCenter
```
private NotificationCenter() {
        Looper looper = Looper.getMainLooper();
        mHandler = new Handler(looper);
    }
```
会创建一个mHandler对象，这个是消息发送的根本。
从其中调用可以看，采用了对象作为第二个参数，消息传递过来，当然是在subscriber对象中去处理业务逻辑。这就解决了个人对EventBus的注解疑惑，毕竟这样做至少从代码层面可以看出无未被使用的代码，而注解的话unused方法Android Studio一直提示，强迫症不喜欢，哈哈哈。下面以此看两种注册方式
```
 /**
     * 订阅消息
     *
     * @param topic      主题key
     * @param subscriber 回调接口
     * @return
     */
    public boolean subscriber(String topic, TopicSubscriber subscriber) {
        if (TextUtils.isEmpty(topic)) {
            throw new IllegalArgumentException(
                    "Topic must not be null or empty");
        }
        if (subscriber == null) {
            throw new IllegalArgumentException(
                    "Event subscriber must not be null");
        }
        return subscribe(topic, subscribersByTopic,
                new WeakReference<TopicSubscriber>(subscriber));
    }
```
```
 public boolean subscriber(Class eventClass, Subscriber subscriber) {
        if (eventClass == null) {
            throw new IllegalArgumentException("Event class must not be null");
        }
        if (subscriber == null) {
            throw new IllegalArgumentException(
                    "Event subscriber must not be null");
        }
        return subscribe(eventClass, subscribersByClass,
                new WeakReference<Subscriber>(subscriber));
    }
```
可以看到两个传入参数基本无差别，topic类型的忽略。以class传入参数的方法我们通过上文可以看到是```NotificationCenter.defaultCenter().subscriber(EventSubscriber.class,eventSubscriber); ```再看EventSubscriber类
 ```
/**
 * FileName: EventSubscriber
 * Author: owen.zhan
 * Date: 2018/10/26 16:52
 */
public class EventSubscriber  {

}
```
其实其根本没做任何特殊处理，就是一个简单的java 类。而eventSubscriber对象则是
 ```
Subscriber<EventSubscriber> eventSubscriber = new Subscriber<EventSubscriber>() {
        @Override
        public void onEvent(EventSubscriber event) {
            NLog.d(NotificationCenter.TAG,"onEvent====");
        }
    };
 ```
由此可见消息返回后均在onEvent方法中去处理业务逻辑。
下面继续回过头来看注册，从上面注册的代码我们可知，最终均是采用方法subscribe ,只是会区分topic还是class类型去注册：
采用Topic来注册
```
 protected boolean subscribe(final Object classTopicOrPatternWrapper,
                                final Map<Object, Object> subscriberMap, final Object subscriber) {
        if (classTopicOrPatternWrapper == null) {
            throw new IllegalArgumentException("Can't subscribe to null.");
        }

        if (subscriber == null) {
            throw new IllegalArgumentException(
                    "Can't subscribe null subscriber to "
                            + classTopicOrPatternWrapper);
        }
        boolean alreadyExists = false;
        Object realSubscriber = subscriber;
        boolean isWeakRef = subscriber instanceof WeakReference;
        if (isWeakRef) {
            realSubscriber = ((WeakReference) subscriber).get();
        }

        boolean isWeakProxySubscriber = false;
        //1
        if (subscriber instanceof ProxySubscriber) {
            ProxySubscriber proxySubscriber = (ProxySubscriber) subscriber;
            isWeakProxySubscriber = proxySubscriber.getReferenceStrength() == ReferenceStrength.WEAK;
            if (isWeakProxySubscriber) {
                realSubscriber = ((ProxySubscriber) subscriber)
                        .getProxiedSubscriber();
            }
        }

        if (isWeakRef && isWeakProxySubscriber) {
            throw new IllegalArgumentException(
                    "ProxySubscribers should always be subscribed strongly.");
        }

        if (realSubscriber == null) {
            return false;
        }
        synchronized (listenerLock) {//2
            //判断该top是否已经被注册
            List currentSubscribers = (List) subscriberMap
                    .get(classTopicOrPatternWrapper);
            if (currentSubscribers == null) {//3
                currentSubscribers = new ArrayList();
                subscriberMap.put(classTopicOrPatternWrapper,
                        currentSubscribers);
            } else {
                for (Iterator iterator = currentSubscribers.iterator(); iterator
                        .hasNext(); ) {
                    Object currentSubscriber = iterator.next();
                             Object realCurrentSubscriber = getRealSubscriberAndCleanStaleSubscriberIfNecessary(
                            iterator, currentSubscriber);
                    if (realSubscriber.equals(realCurrentSubscriber)) {
                        iterator.remove();
                        alreadyExists = true;
                    }
                }
            }

            currentSubscribers.add(subscriber);
            return !alreadyExists;
        }
    }
```
1. 判断是topic注册还是其他类型注册。
2. 判断该对象是否已经被注册，前面部分代码作出基本判断。
```
 private Map subscribersByTopic = new HashMap();//根据topic 来注册的消息
 private Map subscribersByClass = new HashMap();//根据对象来注册的消息
```
## 3.2 发送消息
从上文我们可知消息最终是采用publish方法发出去。
```
 /**
     * 发送消息
     *
     * @param topicName
     * @param eventObj
     */
    public void publish(String topicName, Object eventObj) {
        mHandler.post(new PublishRunnable(null, topicName, eventObj, getSubscribersToTopic(topicName)));
    }
```
```
/**
     * topic 为class
     * @param event
     */
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot publish null event.");
        }
        mHandler.post(new PublishRunnable(event, null, null, getSubscribers(event.getClass())));
    }
```
可以看出最终均是通过handler来处理一个PublishRunnable，我们下面来看看该类：
```
class PublishRunnable implements Runnable {
        Object theEvent;
        String theTopic;
        Object theEventObject;
        List theSubscribers;

        public PublishRunnable(final Object event, final String topic,
                               final Object eventObj, final List subscribers) {
            this.theEvent = event;
            this.theTopic = topic;
            this.theEventObject = eventObj;
            this.theSubscribers = subscribers;
        }

        @Override
        public void run() {
            publish(theEvent, theTopic, theEventObject, theSubscribers);
        }
    }
```
继续往下查看：publish方法：
```
protected void publish(final Object event, final String topic,
                           final Object eventObj, final List subscribers) {
        if (event == null && topic == null) {
            throw new IllegalArgumentException(
                    "Can't publish to null topic/event.");
        }

        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (int i = 0; i < subscribers.size(); i++) {
            Object eh = subscribers.get(i);
            //区分是主题类型还是对象类型
            if (event != null) {
                Subscriber eventSubscriber = (Subscriber) eh;
                try {
                 //1
                    eventSubscriber.onEvent(event);
                } catch (Throwable e) {
                }
            } else {
              //2
                TopicSubscriber eventTopicSubscriber = (TopicSubscriber) eh;
                try {
                    eventTopicSubscriber.onEvent(topic, eventObj);
                } catch (Throwable e) {

                }
            }
        }
    }
```
可以看到该方法是用来遍历前面提到的集合，然后再判断注册的类型，最终到达消息传递的功能。
1. class 注册方式来处理。
2. topic 注册方式来处理。
## 3.3 反注册
```
/**
     *
     * @param cl
     * @param subscriber
     * @return
     */
    public boolean unsubscribe(Class cl, Subscriber subscriber) {
        return unsubscribe(cl, subscribersByClass, subscriber);
    }
```
```
/**
     * 反向注册
     *
     * @param topic
     * @param subscriber
     * @return
     */
    public boolean unsubscribe(String topic, TopicSubscriber subscriber) {
        return unsubscribe(topic, subscribersByTopic, subscriber);
    }
```
可以看到最终均是走向unubscribe方法，那么我们来看看这个方法
 ```
protected boolean unsubscribe(Object o, Map subscriberMap, Object subscriber) {

        if (o == null) {
            throw new IllegalArgumentException("Can't unsubscribe to null.");
        }
        if (subscriber == null) {
            throw new IllegalArgumentException(
                    "Can't unsubscribe null subscriber to " + o);
        }

        synchronized (listenerLock) {
            return removeFromSetResolveWeakReferences(subscriberMap, o,
                    subscriber);
        }
    }
```
可以发现是走向了removeFromSetResolveWeakReferences 方法，继续往下查看
```
private boolean removeFromSetResolveWeakReferences(Map map, Object key,
                                                       Object toRemove) {
        List subscribers = (List) map.get(key);
        if (subscribers == null) {
            return false;
        }
        if (subscribers.remove(toRemove)) {
            if (toRemove instanceof WeakReference) {
                // decWeakRefPlusProxySubscriberCount();
            }
            if (toRemove instanceof ProxySubscriber) {
                ((ProxySubscriber) toRemove).proxyUnsubscribed();
                // decWeakRefPlusProxySubscriberCount();
            }
            return true;
        }

        // search for WeakReferences and ProxySubscribers
        for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
            Object existingSubscriber = iter.next();
            if (existingSubscriber instanceof ProxySubscriber) {
                ProxySubscriber proxy = (ProxySubscriber) existingSubscriber;
                existingSubscriber = proxy.getProxiedSubscriber();
                if (existingSubscriber == toRemove) {
                    removeProxySubscriber(proxy, iter);
                    return true;
                }
            }
            if (existingSubscriber instanceof WeakReference) {
                WeakReference wr = (WeakReference) existingSubscriber;
                Object realRef = wr.get();
                if (realRef == null) {
                    // clean up a garbage collected reference
                    iter.remove();
                    // decWeakRefPlusProxySubscriberCount();
                    return true;
                } else if (realRef == toRemove) {
                    iter.remove();
                    // decWeakRefPlusProxySubscriberCount();
                    return true;
                } else if (realRef instanceof ProxySubscriber) {
                    ProxySubscriber proxy = (ProxySubscriber) realRef;
                    existingSubscriber = proxy.getProxiedSubscriber();
                    if (existingSubscriber == toRemove) {
                        removeProxySubscriber(proxy, iter);
                        return true;
                    }
                }
            }
        }
        return false;
    }
```
整体功能就是判断类型，依次清理上述说的两个集合subscribersByTopic和subscribersByClass。
整NotificationCenter 基本使用和逻辑结构基本如此，详细的可以参照[Demo](https://github.com/devzhan/NotificationBus)去研究和拓展。
# 4 . 总结
  NotificationBus 是根据项目需求，结合EventBus来写的一个消息订阅框架，欢迎各位大神来多多补充和交流。关于其中个人觉得的优缺点作出以下几点简单说明，欢迎拍砖。
 优点：
 1.  整体采用map来管理，最终是遍历集合查找，相对EventBus采用对象绑定，然后最终采用反射来调用，性能方面会更好一点。
2. 没有用注解，个人觉得使用起来业务逻辑清晰。
3. 比EventBus更轻量化。

缺点：
1 .   没有EventBus会区分线程来注册和发布消息，这点不够全面。

以上是根据EventBus 的源码研究和结合项目需求来写的，中间会存在很多问题，希望各位大神多多指教。
















