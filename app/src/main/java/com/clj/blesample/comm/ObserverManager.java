package com.clj.blesample.comm;


import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;


//如何保证一个类只有一个实例并且这个实例易于被访问呢？
//  定义一个全局变量可以确保对象随时都可以被访问，但不能防止我们实例化多个对象。
//  一个更好的解决办法是让类自身负责保存它的唯一实例。
//  这个类可以保证没有其他实例被创建，并且它可以提供一个访问该实例的方法。
//  这就是单例模式的模式动机。

//单例模式有以下特点：
//  1、单例类只能有一个实例。
//  2、单例类必须自己创建自己的唯一实例。
//  3、单例类必须给所有其他对象提供这一实例。

//单例模式的实现方法：
//  1.饿汉式(线程安全，调用效率高，但是不能延时加载，即无论是否用到，都会创建这个单例的对象)
//  2.懒汉式(线程安全，调用效率不高，但是能延时加载，即用到时才会创建这个单例的对象)
//  3.Double CheckLock实现单例：DCL也就是双重锁判断机制（由于JVM底层模型原因，偶尔会出问题，不建议使用）
//  4.静态内部类实现模式（线程安全，调用效率高，可以延时加载）
//  5.枚举类（线程安全，调用效率高，不能延时加载，可以天然的防止反射和反序列化调用）
//单例对象若占用资源少，不需要延时加载，则枚举类 好于 饿汉式
//单例对象若占用资源多，需要延时加载，则静态内部类 好于 懒汉式


//单例模式：静态内部类实现模式（线程安全，调用效率高，可以延时加载）
public class ObserverManager implements Observable {

    public static ObserverManager getInstance() {

        return ObserverManagerHolder.sObserverManager;
    }

    //静态内部类
    //此处利用final实现只new一个实例
    private static class ObserverManagerHolder {
        //类常量static final(即类的多个对象的共享常量)
        private static final ObserverManager sObserverManager = new ObserverManager();
    }

    private List<Observer> observers = new ArrayList<>();

    @Override
    public void addObserver(Observer obj) {
        observers.add(obj);
    }

    @Override
    public void deleteObserver(Observer obj) {
        int i = observers.indexOf(obj);
        if (i >= 0) {
            observers.remove(obj);
        }
    }

    @Override
    public void notifyObserver(BleDevice bleDevice) {
        for (int i = 0; i < observers.size(); i++) {
            Observer o = observers.get(i);
            o.disConnected(bleDevice);
        }
    }

}
