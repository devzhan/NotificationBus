package com.sample.notificatonlibrary.notification;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 消息中心
 *
 * @author owen
 */
public class NotificationCenter {
    public static final String TAG = "NotificationCenter";

    private Map subscribersByTopic = new HashMap();//根据topic 来注册的消息
    private Map subscribersByClass = new HashMap();//根据对象来注册的消息
    private final Object listenerLock = new Object();
    private Handler mHandler;

    private NotificationCenter() {
        Looper looper = Looper.getMainLooper();
        mHandler = new Handler(looper);
    }

    public static NotificationCenter defaultCenter() {
        return CenterInstance._instanceCenter;
    }

    public void setLooper(Looper looper) {
        if (looper == null)
            looper = Looper.getMainLooper();

        if (mHandler.getLooper() == looper)
            return;

        mHandler = new Handler(looper);
    }

    /**
     * 删除所有的订阅者
     */
    public void clearAllSubscribers() {
        synchronized (listenerLock) {
            unsubscribeAllInMap(subscribersByTopic);
            unsubscribeAllInMap(subscribersByClass);
        }
    }

    private void unsubscribeAllInMap(Map subscriberMap) {
        synchronized (listenerLock) {
            Set subscriptionKeys = subscriberMap.keySet();
            for (Object key : subscriptionKeys) {
                List subscribers = (List) subscriberMap.get(key);
                while (!subscribers.isEmpty()) {
                    unsubscribe(key, subscriberMap, subscribers.get(0));
                }
            }
        }
    }

    protected Object getRealSubscriberAndCleanStaleSubscriberIfNecessary(
            Iterator iterator, Object existingSubscriber) {
        ProxySubscriber existingProxySubscriber = null;
        if (existingSubscriber instanceof WeakReference) {
            existingSubscriber = ((WeakReference) existingSubscriber).get();
            if (existingSubscriber == null) {
                iterator.remove();
                // decWeakRefPlusProxySubscriberCount();
            }
        } else if (existingSubscriber instanceof ProxySubscriber) {
            existingProxySubscriber = (ProxySubscriber) existingSubscriber;
            existingSubscriber = existingProxySubscriber.getProxiedSubscriber();
            if (existingSubscriber != null) {
                removeProxySubscriber(existingProxySubscriber, iterator);
            }
        }
        return existingSubscriber;
    }

    protected void removeProxySubscriber(ProxySubscriber proxy, Iterator iter) {
        iter.remove();
        proxy.proxyUnsubscribed();
    }

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
            return false;// already garbage collected? Weird.
        }
        synchronized (listenerLock) {
            //判断该top是否已经被注册
            List currentSubscribers = (List) subscriberMap
                    .get(classTopicOrPatternWrapper);
            if (currentSubscribers == null) {
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

    public boolean subscribeStrongly(Class cl, Subscriber eh) {
        if (eh == null) {
            throw new IllegalArgumentException("Subscriber cannot be null.");
        }
        return subscribe(cl, subscribersByClass, eh);
    }

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

    /**
     * All event subscriber unsubscriptions call this method. Extending classes
     * only have to override this method to subscribe all subscriber
     * unsubscriptions.
     *
     * @param o             the topic or event class to unsubscribe from
     * @param subscriberMap the map of subscribers to use (by topic of class)
     * @param subscriber    the subscriber to unsubscribe, either an EventSubscriber or an
     *                      EventTopicSubscriber, or a WeakReference to either
     * @return boolean if the subscriber is unsubscribed (was subscribed).
     */
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

    public boolean unsubscribe(Class cl, Subscriber subscriber) {
        return unsubscribe(cl, subscribersByClass, subscriber);
    }

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


    private List createCopyOfContentsRemoveWeakRefs(
            Collection subscribersOrVetoListeners) {
        if (subscribersOrVetoListeners == null) {
            return null;
        }
        List copyOfSubscribersOrVetolisteners = new ArrayList(
                subscribersOrVetoListeners.size());
        for (Iterator iter = subscribersOrVetoListeners.iterator(); iter
                .hasNext(); ) {
            Object elem = iter.next();
            if (elem instanceof ProxySubscriber) {
                ProxySubscriber proxy = (ProxySubscriber) elem;
                elem = proxy.getProxiedSubscriber();
                if (elem == null) {
                    removeProxySubscriber(proxy, iter);
                } else {
                    copyOfSubscribersOrVetolisteners.add(proxy);
                }
            } else if (elem instanceof WeakReference) {
                Object hardRef = ((WeakReference) elem).get();
                if (hardRef == null) {
                    // Was reclaimed, unsubscribe
                    iter.remove();
                    // decWeakRefPlusProxySubscriberCount();
                } else {
                    copyOfSubscribersOrVetolisteners.add(hardRef);
                }
            } else {
                copyOfSubscribersOrVetolisteners.add(elem);
            }
        }
        return copyOfSubscribersOrVetolisteners;
    }


    public <T> List<T> getSubscribersToClass(Class<T> eventClass) {
        List result = null;
        final Map classMap = subscribersByClass;
        Set keys = classMap.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext(); ) {
            Class cl = (Class) iterator.next();
            if (cl.isAssignableFrom(eventClass)) {
                Collection subscribers = (Collection) classMap.get(cl);
                if (result == null) {
                    result = new ArrayList();
                }
                result.addAll(createCopyOfContentsRemoveWeakRefs(subscribers));
            }
        }

        return result;
    }

    public <T> List<T> getSubscribers(Class<T> eventClass) {
        synchronized (listenerLock) {
            return getSubscribersToClass(eventClass);
        }
    }


    /**
     * 发送消息
     *
     * @param topicName
     * @param eventObj
     */
    public void publish(String topicName, Object eventObj) {
        mHandler.post(new PublishRunnable(null, topicName, eventObj, getSubscribersToTopic(topicName)));
    }

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

    private List getSubscribers(Object classOrTopic, Map subscriberMap) {
        List result;
        List subscribers = (List) subscriberMap.get(classOrTopic);
        result = createCopyOfContentsRemoveWeakRefs(subscribers);
        return result;
    }

    /**
     * 根据topic来获取事件的消费者
     * @param topic
     * @param <T>
     * @return
     */
    public <T> List<T> getSubscribersToTopic(String topic) {
        synchronized (listenerLock) {
            return getSubscribers(topic, subscribersByTopic);
        }
    }



    /**
     * All publish methods call this method. Extending classes only have to
     * override this method to handle all publishing cases.
     *
     * @param event       the event to publish, null if publishing on a topic
     * @param topic       if publishing on a topic, the topic to publish on, else null
     * @param eventObj    if publishing on a topic, the eventObj to publish, else null
     * @param subscribers the subscribers to publish to - must be a snapshot copy
     * @throws IllegalArgumentException if eh or o is null
     */
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
                    eventSubscriber.onEvent(event);
                } catch (Throwable e) {

                }
            } else {
                TopicSubscriber eventTopicSubscriber = (TopicSubscriber) eh;
                try {
                    eventTopicSubscriber.onEvent(topic, eventObj);
                } catch (Throwable e) {

                }
            }
        }
    }

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

    private static class CenterInstance {
        private static NotificationCenter _instanceCenter = new NotificationCenter();
    }
}
