/*
 * Copyright (c) 2016 Nick Guo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nick.eventbus.internal;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.nick.scalpel.core.opt.SharedExecutor;

import java.util.ArrayList;
import java.util.List;

import dev.nick.eventbus.Event;
import dev.nick.eventbus.EventReceiver;

/**
 * Created by nick on 16-4-1.
 * Email: nick.guo.dev@icloud.com
 */
public class PublisherService implements Publisher, Subscriber {

    private final List<EventReceiver> mEventReceivers;

    private Handler mMainHandler;

    PublisherService() {
        mEventReceivers = new ArrayList<>();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void publish(@NonNull final Event event) {
        synchronized (mEventReceivers) {
            for (final EventReceiver receiver : mEventReceivers) {
                int[] events = receiver.events();
                for (int e : events) {
                    if (e == event.getEventType()) {
                        if (receiver.callInMainThread())
                            runOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    receiver.onReceive(Event.fromClone(event));
                                }
                            });
                        else
                            runOnWorkerThread(new Runnable() {
                                @Override
                                public void run() {
                                    receiver.onReceive(Event.fromClone(event));
                                }
                            });
                        break;
                    }
                }
            }
        }
    }

    void runOnMainThread(Runnable runnable) {
        mMainHandler.post(runnable);
    }

    void runOnWorkerThread(Runnable runnable) {
        SharedExecutor.get().execute(runnable);
    }

    @Override
    public void subscribe(@NonNull EventReceiver receiver) {
        synchronized (mEventReceivers) {
            if (!mEventReceivers.contains(receiver)) mEventReceivers.add(receiver);
        }
    }
}