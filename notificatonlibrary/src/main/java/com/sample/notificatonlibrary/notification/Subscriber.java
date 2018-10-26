package com.sample.notificatonlibrary.notification;

/**
 * 对象型消息订阅者接口
 *
 * @param <T>
 */
public interface Subscriber<T>
{
	/**
      * Handle a published event. 
      *
      * @param event The Object that is being published.
    **/
	public void onEvent(T event);
}
