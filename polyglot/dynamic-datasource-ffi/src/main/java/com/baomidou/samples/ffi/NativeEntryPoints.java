/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
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
 */
package com.baomidou.samples.ffi;

import com.baomidou.samples.ffi.DynamicDataSourceFacade.Operation;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.Isolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CTypeConversion;

/**
 * libdynamicds 的 C ABI（构建时生成 {@code libdynamicds.h}）。
 *
 * <p>前四个函数管理 isolate 与线程：进程内调用一次 {@code dd_create_isolate}，其他 OS 线程先 {@code dd_attach_thread}
 * 再调用业务函数。业务函数的签名统一为 {@code char* dd_xxx(graal_isolatethread_t* thread, const char*
 * request)}，请求与返回值都是 NUL 结尾的 UTF-8 JSON，返回值必须交给 {@code dd_free} 释放。
 */
public final class NativeEntryPoints {

    private NativeEntryPoints() {}

    /** 创建 isolate，调用线程随之 attach；进程内只需调用一次。 */
    @CEntryPoint(name = "dd_create_isolate", builtin = CEntryPoint.Builtin.CREATE_ISOLATE)
    static native IsolateThread createIsolate();

    /** 取 isolate 句柄，交给其他线程 attach。 */
    @CEntryPoint(name = "dd_get_isolate", builtin = CEntryPoint.Builtin.GET_ISOLATE)
    static native Isolate getIsolate(IsolateThread thread);

    /** 把当前 OS 线程 attach 到 isolate，对已经 attach 的线程是幂等的。 */
    @CEntryPoint(name = "dd_attach_thread", builtin = CEntryPoint.Builtin.ATTACH_THREAD)
    static native IsolateThread attachThread(Isolate isolate);

    /** 线程退出前 detach。 */
    @CEntryPoint(name = "dd_detach_thread", builtin = CEntryPoint.Builtin.DETACH_THREAD)
    static native int detachThread(IsolateThread thread);

    @CEntryPoint(name = "dd_open")
    static CCharPointer open(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::open, request);
    }

    @CEntryPoint(name = "dd_close")
    static CCharPointer close(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::close, request);
    }

    @CEntryPoint(name = "dd_execute")
    static CCharPointer execute(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::execute, request);
    }

    @CEntryPoint(name = "dd_transaction")
    static CCharPointer transaction(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::transaction, request);
    }

    @CEntryPoint(name = "dd_add_datasource")
    static CCharPointer addDataSource(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::addDataSource, request);
    }

    @CEntryPoint(name = "dd_remove_datasource")
    static CCharPointer removeDataSource(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::removeDataSource, request);
    }

    @CEntryPoint(name = "dd_list_datasources")
    static CCharPointer listDataSources(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::listDataSources, request);
    }

    @CEntryPoint(name = "dd_tx_begin")
    static CCharPointer beginTransaction(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::beginTransaction, request);
    }

    @CEntryPoint(name = "dd_tx_execute")
    static CCharPointer executeInTransaction(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::executeInTransaction, request);
    }

    @CEntryPoint(name = "dd_tx_commit")
    static CCharPointer commitTransaction(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::commitTransaction, request);
    }

    @CEntryPoint(name = "dd_tx_rollback")
    static CCharPointer rollbackTransaction(IsolateThread thread, @CConst CCharPointer request) {
        return call(DynamicDataSourceFacade::rollbackTransaction, request);
    }

    /** 释放业务函数返回的字符串，传入 NULL 是安全的。 */
    @CEntryPoint(name = "dd_free")
    static void free(IsolateThread thread, CCharPointer pointer) {
        UnmanagedMemory.free(pointer);
    }

    private static CCharPointer call(Operation operation, CCharPointer request) {
        // JDK 18 起默认字符集固定为 UTF-8（JEP 400），toJavaString 按它解码
        var json = request.isNull() ? "" : CTypeConversion.toJavaString(request);
        return toNativeString(DynamicDataSourceFacade.call(operation, json));
    }

    /** 复制到 malloc 出来的内存里：所有权交给调用方，由 {@code dd_free} 释放。 */
    private static CCharPointer toNativeString(String value) {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        CCharPointer pointer = UnmanagedMemory.malloc(bytes.length + 1);
        CTypeConversion.asByteBuffer(pointer, bytes.length).put(bytes);
        pointer.write(bytes.length, (byte) 0);
        return pointer;
    }
}
