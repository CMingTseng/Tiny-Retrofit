package com.github.ganquan.tiny.retrofit;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import io.reactivex.observers.DisposableSingleObserver;

/**
 * 继承该类后不需要手动调用dispose取消对事件源的监听，会在被绑定的生命周期结束后自动解绑
 *
 * @author GanQuan
 * @since 2018/3/14.
 */
public abstract class JtSingObserver<T> extends DisposableSingleObserver<T> implements LifecycleObserver {
    /**
     * 如果非viewModel内调用网络请求，需要传LifecycleOwner对象
     * 使其在生命周期结束时释放rx事件源观察
     *
     * @param binder
     */
    public JtSingObserver(Context binder) {
        if (binder instanceof LifecycleOwner) {
            ((LifecycleOwner) binder).getLifecycle().addObserver(this);
        }

    }

    /**
     * viewModel中使用，不需要主动释放（没持有Activity）
     */
    public JtSingObserver() {
        super();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onClear() {
        if (isDisposed()) {
            return;
        }
        this.dispose();
    }

}
