package com.xiaojing.distributed.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 序列化,直接使用java的ObjectOutputStream.writeObject,后续可以优化
 */
public class DistributedTransInvocation implements Serializable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DistributedTransInvocation.class);
  private String className;
  private String methodName;
  private Class<?>[] parameterTypes;
  private Object[] parameters;

  public DistributedTransInvocation(String className, String methodName,
                                    Class<?>[] parameterTypes, Object[] parameters) {
    this.className = className;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.parameters = parameters;
  }

  public static byte[] serialize(DistributedTransInvocation distributedTransInvocation)
      throws IOException {
    ObjectOutputStream objectOutputStream = null;
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(distributedTransInvocation);
      return byteArrayOutputStream.toByteArray();
    } finally {
      if (objectOutputStream != null) {
        objectOutputStream.close();
      }
    }
  }

  public static DistributedTransInvocation deserialize(byte[] data)
      throws IOException, ClassNotFoundException {
    ObjectInputStream objectInputStream = null;
    try {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
      objectInputStream = new ObjectInputStream(byteArrayInputStream);
      return (DistributedTransInvocation) objectInputStream.readObject();
    } finally {
      if (objectInputStream != null) {
        objectInputStream.close();
      }
    }
  }

  public Object invoke() throws ClassNotFoundException, InvocationTargetException,
                                IllegalAccessException, NoSuchMethodException {
    try {
      Class<?> clazz = Class.forName(this.className);
      Object instance = GuiceConfig.getInstance().getInjector().getInstance(clazz);
      Method method = clazz.getMethod(methodName, parameterTypes);
      return method.invoke(instance, parameters);
    } catch (Throwable throwable) {
      LOGGER.error("Exception in invoke:", throwable);
      throw throwable;
    }
  }
}

