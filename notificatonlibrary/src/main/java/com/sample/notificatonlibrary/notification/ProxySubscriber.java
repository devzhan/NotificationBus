package com.sample.notificatonlibrary.notification;


public interface ProxySubscriber {

   public Object getProxiedSubscriber();


   public void proxyUnsubscribed();

   public ReferenceStrength getReferenceStrength();
}
