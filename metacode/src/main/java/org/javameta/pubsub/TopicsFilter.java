package org.javameta.pubsub;

import java.util.Objects;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class TopicsFilter implements Filter {

    private String[] topics;

    public TopicsFilter(String... topics) {
        this.topics = topics;
    }

    @Override
    public boolean accepts(Object master, String methodName, Message msg) {
        String topic = msg.getTopic();
        if (topic == null)
            return false;

        for (String s : topics)
            if (Objects.equals(topic, s))
                return true;

        return false;
    }
}
