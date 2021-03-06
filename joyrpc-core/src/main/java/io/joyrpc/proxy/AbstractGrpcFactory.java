package io.joyrpc.proxy;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.exception.ProxyException;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.GrpcType.ClassWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.joyrpc.util.ClassUtils.isJavaClass;

/**
 * 抽象的gRPC工厂类
 */
public abstract class AbstractGrpcFactory implements GrpcFactory {

    public static final String REQUEST_SUFFIX = "Request";
    public static final String RESPONSE_SUFFIX = "ResponsePayload";

    @Override
    public GrpcType generate(final Class<?> clz, final Method method) throws ProxyException {
        try {
            ClassWrapper request = getRequestWrapper(clz, method, () -> getRequestClassName(clz, method));
            ClassWrapper response = getResponseWrapper(clz, method, () -> getResponseClassName(clz, method));
            return new GrpcType(request, response);
        } catch (ProxyException e) {
            throw e;
        } catch (Exception e) {
            throw new ProxyException(String.format("Error occurs while building grpcType of %s.%s",
                    clz.getName(), method.getName()), e);
        }
    }

    /**
     * 构建请求对象类名
     *
     * @param clz    类
     * @param method 方法
     * @return 请求对象类名
     */
    protected String getRequestClassName(final Class<?> clz, final Method method) {
        String methodName = method.getName();
        return new StringBuilder(100)
                .append(clz.getName()).append('$')
                .append(Character.toUpperCase(methodName.charAt(0)))
                .append(methodName.substring(1))
                .append(REQUEST_SUFFIX)
                .toString();
    }

    /**
     * 构建应答对象类名
     *
     * @param clz    类
     * @param method 方法
     * @return 应答对象类名
     */
    protected String getResponseClassName(final Class<?> clz, final Method method) {
        String methodName = method.getName();
        return new StringBuilder(100)
                .append(clz.getName()).append('$')
                .append(Character.toUpperCase(methodName.charAt(0)))
                .append(methodName.substring(1))
                .append(RESPONSE_SUFFIX).toString();
    }

    /**
     * 获取应答包装类型
     *
     * @param clz    类
     * @param method 方法
     * @param naming 方法名称提供者
     * @return
     * @throws Exception
     */
    protected ClassWrapper getResponseWrapper(final Class<?> clz, final Method method, final Supplier<String> naming) throws Exception {
        Class clazz = method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(clz)) {
            //异步支持
            Type type = method.getGenericReturnType();
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            type = actualTypeArguments[0];
            if (type instanceof Class) {
                clazz = (Class) type;
            } else {
                throw new ProxyException(String.format("unsupported generic type of %s.%s", clz.getName(), method.getName()));
            }
        }
        if (clazz == void.class) {
            return null;
        } else if (isPojo(clazz)) {
            return new ClassWrapper(clazz, false);
        } else {
            return new ClassWrapper(buildResponseClass(clz, method, naming), true);
        }
    }

    /**
     * 包装应答类型
     *
     * @param method 方法
     * @param naming 方法名称提供者
     * @return
     * @throws Exception
     */
    protected abstract Class<?> buildResponseClass(Class<?> clz, Method method, Supplier<String> naming) throws Exception;

    /**
     * 构建请求包装类型
     *
     * @param clz    类
     * @param method 方法
     * @param naming 方法名称提供者
     * @return 包装的类
     * @throws Exception 异常
     */
    protected ClassWrapper getRequestWrapper(final Class<?> clz, final Method method, final Supplier<String> naming) throws Exception {
        Parameter[] parameters = method.getParameters();
        switch (parameters.length) {
            case 0:
                return null;
            case 1:
                Class<?> clazz = parameters[0].getType();
                if (isPojo(clazz)) {
                    return new ClassWrapper(clazz, false);
                }
            default:
                return new ClassWrapper(buildRequestClass(clz, method, naming), true);
        }
    }

    /**
     * 构建请求类型
     *
     * @param method 方法
     * @param naming 方法名称提供者
     * @return
     * @throws Exception
     */
    protected abstract Class<?> buildRequestClass(final Class<?> clz, final Method method, final Supplier<String> naming) throws Exception;

    /**
     * 是否是POJO类
     *
     * @param clazz 类
     * @return
     */
    protected boolean isPojo(final Class<?> clazz) {
        return !isJavaClass(clazz);
    }

}
