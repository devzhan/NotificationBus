package com.sample.notificatonlibrary.notification;
/**
 * 主题型消息订阅接口
 * @param <T>
 */
public interface TopicSubscriber<T>
{
	/**
	 *
	 * @param topic
	 *            the name of the topic published on
	 * @param data
	 *            the data object published on the topic
	 */
	public void onEvent(String topic, T data);
}
