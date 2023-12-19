package com.sy.pangu.common.lock.zk;

import cn.hutool.core.io.FileUtil;
import javassist.*;
import javassist.bytecode.BadBytecode;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.asm.ClassReader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author cheng.wang
 * @time 2023/12/15 15:27
 * @des 更改InterProcessMutex#release方法
 */
@Slf4j
public class ModifyInterProcessMutexRelease {

    private static Class<?> INTERPROCESSMUTEX_CLASS;

    /**
     * 修改InterProcessMutex#release()方法，添加参数(Thread currentThread)方法
     * 给予其他线程释放锁的能力
     */
    @Deprecated
    public static void modifyReleaseMethod(){
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get("org.apache.curator.framework.recipes.locks.InterProcessMutex");
            CtMethod releaseMethod = ctClass.getDeclaredMethod("release");
            releaseMethod.addParameter(classPool.get("java.lang.Thread"));
            // 修改方法体代码
            releaseMethod.setBody(
                    "{\n" +
                            "    java.lang.Thread currentThread = $1;\n" +
                            "    org.apache.curator.framework.recipes.locks.InterProcessMutex.LockData lockData = $0.threadData.get(currentThread);\n" +
                            "    if (lockData == null) {\n" +
                            "        throw new java.lang.IllegalMonitorStateException(\"You do not own the lock: \" + $0.basePath);\n" +
                            "    }\n" +
                            "    int newLockCount = lockData.lockCount.decrementAndGet();\n" +
                            "    if (newLockCount > 0) {\n" +
                            "        return;\n" +
                            "    }\n" +
                            "    if (newLockCount < 0) {\n" +
                            "        throw new java.lang.IllegalMonitorStateException(\"Lock count has gone negative for lock: \" + $0.basePath);\n" +
                            "    }\n" +
                            "    try {\n" +
                            "        $0.internals.releaseLock(lockData.lockPath);\n" +
                            "    } finally {\n" +
                            "        $0.threadData.remove(currentThread);\n" +
                            "    }\n" +
                            "}"
            );

            // 保存修改后的类文件
            String targetClassPath = Thread.currentThread().getContextClassLoader().getResource("").toURI().getPath();
            ctClass.toBytecode();
            ctClass.writeFile(targetClassPath);
            INTERPROCESSMUTEX_CLASS = ctClass.toClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("InterProcessMutex modify modified successfully!");
    }

    /**
     * 给InterProcessMutex添加release(Thread currentThread)方法
     * 给予其他线程释放锁的能力
     */
    public static void addReleaseMethod(){
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get("org.apache.curator.framework.recipes.locks.InterProcessMutex");

            String methodName = "release";
            CtClass[] parameterTypes = { classPool.get("java.lang.Thread") };  // 方法参数类型
            CtMethod releaseMethod = new CtMethod(CtClass.voidType, methodName, parameterTypes, ctClass);
            releaseMethod.setModifiers(Modifier.PUBLIC);
            String methodBody = "{\n" +
                    "    java.lang.Thread currentThread = $1;\n" +
                    "    org.apache.curator.framework.recipes.locks.InterProcessMutex.LockData lockData = $0.threadData.get(currentThread);\n" +
                    "    if (lockData == null) {\n" +
                    "        throw new java.lang.IllegalMonitorStateException(\"You do not own the lock: \" + $0.basePath);\n" +
                    "    }\n" +
                    "    int newLockCount = lockData.lockCount.decrementAndGet();\n" +
                    "    if (newLockCount > 0) {\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    if (newLockCount < 0) {\n" +
                    "        throw new java.lang.IllegalMonitorStateException(\"Lock count has gone negative for lock: \" + $0.basePath);\n" +
                    "    }\n" +
                    "    try {\n" +
                    "        $0.internals.releaseLock(lockData.lockPath);\n" +
                    "    } finally {\n" +
                    "        $0.threadData.remove(currentThread);\n" +
                    "    }\n" +
                    "}";
            releaseMethod.setBody(methodBody);
            ctClass.addMethod(releaseMethod);
            // 保存修改后的类文件
            ctClass.toBytecode();
            ctClass.writeFile();
            INTERPROCESSMUTEX_CLASS = ctClass.toClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("InterProcessMutex add modified successfully!");
    }


    public static Object getInstance(CuratorFramework curatorFramework, String path){
        try {
            Constructor<?> constructor = INTERPROCESSMUTEX_CLASS.getConstructor(CuratorFramework.class, String.class);
            return constructor.newInstance(curatorFramework, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(){
        try {
            FileUtil.del("org");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
