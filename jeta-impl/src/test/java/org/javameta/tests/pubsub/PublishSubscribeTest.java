/*
 * Copyright 2015 Oleg Khalidov
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.javameta.tests.pubsub;

import org.javameta.BaseTest;
import org.javameta.Logger;
import org.javameta.TestMetaHelper;
import org.javameta.log.Log;
import org.javameta.pubsub.*;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class PublishSubscribeTest extends BaseTest {

    @Log
    Logger logger;

    public static class PublisherHolder {
        @Publish
        protected Subscribers<MessageOne> subscribers;
    }

    public static class PublisherTwoHolder {
        @Publish
        protected Subscribers<MessageTwo> subscribers;
    }

    public static class SubscribeHolder {
        @Log
        Logger logger;

        volatile int onMessageOneInvokes = 0;
        volatile MessageOne lastMessageOne;

        volatile int onMessageTwoInvokes = 0;
        volatile MessageTwo lastMessageTwo;

        public SubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(PublisherHolder.class)
        void onMessageOne(MessageOne message) {
            logger.debug("onMessageOne(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneInvokes++;
            lastMessageOne = message;
        }

        @Subscribe(PublisherTwoHolder.class)
        void onMessageTwo(MessageTwo message) {
            logger.debug("onMessageTwo(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageTwoInvokes++;
            lastMessageTwo = message;
        }
    }

    @Test
    public void testSimpleNotify() {
        logger.debug("testSimpleNotify()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);
        SubscribeHolder subscriber = new SubscribeHolder();
        SubscriptionHandler handler = TestMetaHelper.registerSubscriber(subscriber);

        publisher.subscribers.notify(new MessageOne(1, "one"));
        assertThat(subscriber.onMessageOneInvokes, is(1));
        assertThat(subscriber.lastMessageOne.getTopic(), is("one"));
        assertThat(subscriber.lastMessageOne.getId(), is(1));

        handler.unregisterAll();
        publisher.subscribers.notify(new MessageOne(1, "none"));
        assertThat(subscriber.onMessageOneInvokes, is(1));
    }

    @Test
    public void testAsyncNotify() {
        logger.debug("testAsyncNotify()");

        final PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);
        final PublisherTwoHolder publisherTwo = new PublisherTwoHolder();
        TestMetaHelper.createPublisher(publisherTwo);

        SubscribeHolder subscriber = new SubscribeHolder();
        SubscriptionHandler handler = TestMetaHelper.registerSubscriber(subscriber);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    publisher.subscribers.notify(new MessageOne(1, "one"));
                    publisherTwo.subscribers.notify(new MessageTwo(2, "two"));
                }
            });
        }

        for (Thread thread : threads)
            thread.start();

        sleepQuietly(500);

        assertThat(subscriber.onMessageOneInvokes, is(10));
        assertThat(subscriber.onMessageTwoInvokes, is(10));
        assertThat(subscriber.lastMessageTwo.getId(), is(2));
        handler.unregisterAll();
    }

    public static class HighPrioritySubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneInvokes = 0;

        public HighPrioritySubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, priority = Integer.MAX_VALUE)
        void onMessageOne(MessageOne message) {
            logger.debug("onMessageOne(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneInvokes++;
            assertThat(message.getId(), is(1));
            message.incId();
        }
    }

    public static class LowPrioritySubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneInvokes = 0;

        public LowPrioritySubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, priority = Integer.MIN_VALUE)
        void onMessageOne(MessageOne message) {
            logger.debug("onMessageOne(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneInvokes++;
            assertThat(message.getId(), is(2));
        }
    }

    @Test
    public void testPriority() {
        logger.debug("testPriority()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);

        LowPrioritySubscribeHolder low = new LowPrioritySubscribeHolder();
        TestMetaHelper.registerSubscriber(low);

        publisher.subscribers.notify(new MessageOne(2, "one"));
        assertThat(low.onMessageOneInvokes, is(1));

        HighPrioritySubscribeHolder high = new HighPrioritySubscribeHolder();
        TestMetaHelper.registerSubscriber(high);
        publisher.subscribers.notify(new MessageOne(1, "two"));
        assertThat(high.onMessageOneInvokes, is(1));
        assertThat(low.onMessageOneInvokes, is(2));

        publisher.subscribers.clear();
    }

    public static class IdFilterSubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneId1Invokes = 0;
        int onMessageOneId2Invokes = 0;

        public IdFilterSubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, ids = 1)
        void onMessageOneId1(MessageOne message) {
            logger.debug("onMessageOneId1(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneId1Invokes++;
            assertThat(message.getId(), is(1));
        }

        @Subscribe(value = PublisherHolder.class, ids = {2, 4})
        void onMessageOneId2(MessageOne message) {
            logger.debug("onMessageOneId2(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneId2Invokes++;
            assertThat(message.getId(), anyOf(is(2), is(4)));
        }
    }

    @Test
    public void testIdFilter() {
        logger.debug("testIdFilter()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);

        IdFilterSubscribeHolder subscriber = new IdFilterSubscribeHolder();
        TestMetaHelper.registerSubscriber(subscriber);

        publisher.subscribers.notify(new MessageOne(1, "one"));
        assertThat(subscriber.onMessageOneId1Invokes, is(1));
        assertThat(subscriber.onMessageOneId2Invokes, is(0));

        publisher.subscribers.notify(new MessageOne(2, "two"));
        assertThat(subscriber.onMessageOneId1Invokes, is(1));
        assertThat(subscriber.onMessageOneId2Invokes, is(1));

        publisher.subscribers.notify(new MessageOne(3, "none"));
        assertThat(subscriber.onMessageOneId1Invokes, is(1));
        assertThat(subscriber.onMessageOneId2Invokes, is(1));

        publisher.subscribers.notify(new MessageOne(4, "four"));
        assertThat(subscriber.onMessageOneId1Invokes, is(1));
        assertThat(subscriber.onMessageOneId2Invokes, is(2));

        publisher.subscribers.clear();
    }

    public static class TopicFilterSubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneTopicOneInvokes = 0;
        int onMessageOneTopicTwoInvokes = 0;

        public TopicFilterSubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, topics = "one")
        void onMessageOneTopicOne(MessageOne message) {
            logger.debug("onMessageOneTopicOne(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneTopicOneInvokes++;
            assertThat(message.getId(), is(1));
        }

        @Subscribe(value = PublisherHolder.class, topics = {"two", "four"})
        void onMessageOneTopicTwo(MessageOne message) {
            logger.debug("onMessageOneTopicTwo(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneTopicTwoInvokes++;
            assertThat(message.getId(), anyOf(is(2), is(4)));
        }
    }

    @Test
    public void testTopicFilter() {
        logger.debug("testTopicFilter()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);

        TopicFilterSubscribeHolder subscriber = new TopicFilterSubscribeHolder();
        TestMetaHelper.registerSubscriber(subscriber);

        publisher.subscribers.notify(new MessageOne(1, "one"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(0));

        publisher.subscribers.notify(new MessageOne(2, "two"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(3, "none"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(4, "four"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(2));

        publisher.subscribers.notify(new MessageOne(5, "twofour"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(2));

        publisher.subscribers.notify(new MessageOne(6, "two "));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(2));

        publisher.subscribers.notify(new MessageOne(7, " four"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(2));

        publisher.subscribers.notify(new MessageOne(8, "tw.*"));
        assertThat(subscriber.onMessageOneTopicOneInvokes, is(1));
        assertThat(subscriber.onMessageOneTopicTwoInvokes, is(2));

        publisher.subscribers.clear();
    }

    public static class OddIdFilter implements Filter {
        @Override
        public boolean accepts(Object master, String methodName, Message msg) {
            return msg.getId() % 2 != 0;
        }
    }

    public static class EvenIdFilter implements Filter {
        @Override
        public boolean accepts(Object master, String methodName, Message msg) {
            return msg.getId() % 2 == 0;
        }
    }

    public static class CustomFilterSubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneOddInvokes = 0;
        int onMessageOneEventInvokes = 0;

        public CustomFilterSubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, filters = OddIdFilter.class)
        void onMessageOneOdd(MessageOne message) {
            logger.debug("onMessageOneOdd(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneOddInvokes++;
            assertThat(message.getId() % 2, is(not(0)));
        }

        @Subscribe(value = PublisherHolder.class, filters = EvenIdFilter.class)
        void onMessageOneEvent(MessageOne message) {
            logger.debug("onMessageOneEvent(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneEventInvokes++;
            assertThat(message.getId() % 2, is(0));
        }

        @Subscribe(value = PublisherHolder.class, filters = {OddIdFilter.class, EvenIdFilter.class})
        void onMessageOneNone(MessageOne message) {
            logger.debug("onMessageOneNone(id: %d, topic: %s)", message.getId(), message.getTopic());
            assertThat(true, is(false));
        }
    }

    @Test
    public void testCustomFilter() {
        logger.debug("testCustomFilter()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);

        CustomFilterSubscribeHolder subscriber = new CustomFilterSubscribeHolder();
        TestMetaHelper.registerSubscriber(subscriber);

        publisher.subscribers.notify(new MessageOne(1, "one"));
        assertThat(subscriber.onMessageOneOddInvokes, is(1));
        assertThat(subscriber.onMessageOneEventInvokes, is(0));

        publisher.subscribers.notify(new MessageOne(2, "two"));
        assertThat(subscriber.onMessageOneOddInvokes, is(1));
        assertThat(subscriber.onMessageOneEventInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(3, "none"));
        assertThat(subscriber.onMessageOneOddInvokes, is(2));
        assertThat(subscriber.onMessageOneEventInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(4, "four"));
        assertThat(subscriber.onMessageOneOddInvokes, is(2));
        assertThat(subscriber.onMessageOneEventInvokes, is(2));

        publisher.subscribers.clear();
    }

    @MetaFilter(emitExpression = "$e.getId() % 2 != 0")
    public interface OddIdMetaFilter extends Filter {
    }

    @MetaFilter(emitExpression = "$e.getId() % 2 == 0")
    public interface EvenIdMetaFilter extends Filter {
    }

    public static class MetaFilterSubscribeHolder {
        @Log
        Logger logger;

        int onMessageOneOddInvokes = 0;
        int onMessageOneEventInvokes = 0;

        public MetaFilterSubscribeHolder() {
            TestMetaHelper.createLogger(this);
        }

        @Subscribe(value = PublisherHolder.class, filters = OddIdMetaFilter.class)
        void onMessageOneOdd(MessageOne message) {
            logger.debug("onMessageOneOdd(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneOddInvokes++;
            assertThat(message.getId() % 2, is(not(0)));
        }

        @Subscribe(value = PublisherHolder.class, filters = EvenIdMetaFilter.class)
        void onMessageOneEvent(MessageOne message) {
            logger.debug("onMessageOneEvent(id: %d, topic: %s)", message.getId(), message.getTopic());
            onMessageOneEventInvokes++;
            assertThat(message.getId() % 2, is(0));
        }

        @Subscribe(value = PublisherHolder.class, filters = {OddIdMetaFilter.class, EvenIdMetaFilter.class})
        void onMessageOneNone(MessageOne message) {
            logger.debug("onMessageOneNone(id: %d, topic: %s)", message.getId(), message.getTopic());
            assertThat(true, is(false));
        }
    }

    @Test
    public void testMetaFilter() {
        logger.debug("testMetaFilter()");

        PublisherHolder publisher = new PublisherHolder();
        TestMetaHelper.createPublisher(publisher);

        MetaFilterSubscribeHolder subscriber = new MetaFilterSubscribeHolder();
        TestMetaHelper.registerSubscriber(subscriber);

        publisher.subscribers.notify(new MessageOne(1, "one"));
        assertThat(subscriber.onMessageOneOddInvokes, is(1));
        assertThat(subscriber.onMessageOneEventInvokes, is(0));

        publisher.subscribers.notify(new MessageOne(2, "two"));
        assertThat(subscriber.onMessageOneOddInvokes, is(1));
        assertThat(subscriber.onMessageOneEventInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(3, "none"));
        assertThat(subscriber.onMessageOneOddInvokes, is(2));
        assertThat(subscriber.onMessageOneEventInvokes, is(1));

        publisher.subscribers.notify(new MessageOne(4, "four"));
        assertThat(subscriber.onMessageOneOddInvokes, is(2));
        assertThat(subscriber.onMessageOneEventInvokes, is(2));

        publisher.subscribers.clear();
    }
}