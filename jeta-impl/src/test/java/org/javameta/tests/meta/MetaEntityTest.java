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

package org.javameta.tests.meta;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.javameta.BaseTest;
import org.javameta.Logger;
import org.javameta.TestMetaHelper;
import org.javameta.base.Meta;
import org.javameta.base.MetaEntity;
import org.javameta.log.Log;
import org.javameta.util.Constructor;
import org.javameta.util.Factory;
import org.javameta.util.Lazy;
import org.javameta.util.Provider;
import org.junit.Test;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class MetaEntityTest extends BaseTest {

    @Log
    Logger logger;
    
    @MetaEntity
    public static class MetaEntityOne {
        String value = "one";
    }
    
    public static class MetaEntityHolder {
        @Meta
        MetaEntityOne entity;
        @Meta
        Class<? extends MetaEntityOne> clazz;
        @Meta
        Lazy<MetaEntityOne> lazy;
        @Meta
        Provider<MetaEntityOne> provider;
    }

    @Test
	public void testSimpleInject() {
        logger.debug("testSimpleInject()");

        MetaEntityHolder holder = new MetaEntityHolder();
        TestMetaHelper.injectMeta(holder);
        assertThat(holder.entity, notNullValue());
        assertThat(holder.entity.value, is("one"));
        
        assertThat(holder.clazz, notNullValue());
        assertEquals(holder.clazz, MetaEntityOne.class);
        
        assertThat(holder.lazy, notNullValue());
        assertThat(holder.lazy.get().value, is("one"));
        
        assertThat(holder.provider, notNullValue());
        assertThat(holder.provider.get().value, is("one"));
    }

    public static class MetaEntityTwo {
        String value;
    }
    
    @MetaEntity(of=MetaEntityTwo.class)
    public static class MetaEntityTwoProvider {
        @Constructor
        public MetaEntityTwo get() {
            MetaEntityTwo e = new MetaEntityTwo();
            e.value = "two";
            return e;
        }
    }

    public static class MetaEntityThree {
        String value;
    }
    
    @MetaEntity(of=MetaEntityThree.class)
    public static class MetaEntityThreeProvider {
        @Constructor
        public static MetaEntityThree get() {
            MetaEntityThree e = new MetaEntityThree();
            e.value = "three";
            return e;
        }
    }

    public static class MetaEntityFour {
        String value;
    }
    
    @MetaEntity(of=MetaEntityFour.class, staticConstructor="getInstance")
    public static class MetaEntityFourProvider {

        public static MetaEntityFourProvider getInstance() {
            return new MetaEntityFourProvider();   
        }

        @Constructor
        public MetaEntityFour get() {
            MetaEntityFour e = new MetaEntityFour();
            e.value = "four";
            return e;
        }
    }
    
    public static class MetaProviderHolder {
        @Meta
        MetaEntityTwo entityTwo;
        @Meta
        MetaEntityThree entityThree;
        @Meta
        MetaEntityFour entityFour;
    }

    @Test
	public void testMetaProvider() {
        logger.debug("testMetaProvider()");

        MetaProviderHolder holder = new MetaProviderHolder();
        TestMetaHelper.injectMeta(holder);
        assertThat(holder.entityTwo, notNullValue());
        assertThat(holder.entityTwo.value, is("two"));
        logger.debug("entityTwo.value: %s", holder.entityTwo.value);

        assertThat(holder.entityThree, notNullValue());
        assertThat(holder.entityThree.value, is("three"));
        logger.debug("entityThree.value: %s", holder.entityThree.value);

        assertThat(holder.entityFour, notNullValue());
        assertThat(holder.entityFour.value, is("four"));
        logger.debug("entityFour.value: %s", holder.entityFour.value);
    }

    @MetaEntity
    public static class MetaEntityFive {
        String value;

        public MetaEntityFive(String value) { 
            this.value = value;
        }
    }

    public static class MetaFactoryHolder {
        @Meta 
        MetaFactory factory;

        @Factory
        public interface MetaFactory {
            MetaEntityFive get(String value);
            Class<? extends MetaEntityFive> getClazz();
            Lazy<MetaEntityFive> getLazy(String value);
            Provider<MetaEntityFive> getProvider(String value);
        }
    }

    @Test
	public void testMetaFactory() {
        logger.debug("testMetaFactory()");

        MetaFactoryHolder holder = new MetaFactoryHolder();
        TestMetaHelper.injectMeta(holder);
        assertThat(holder.factory, notNullValue());
        assertThat(holder.factory.get(null), notNullValue());
        assertThat(holder.factory.get("five").value, is("five"));
        
        assertThat(holder.factory.getClazz(), notNullValue());
        assertEquals(holder.factory.getClazz(), MetaEntityFive.class);
        
        assertThat(holder.factory.getLazy(null), notNullValue());
        assertThat(holder.factory.getLazy("lazyFive").get().value, is("lazyFive"));
        
        assertThat(holder.factory.getProvider(null), notNullValue());
        assertThat(holder.factory.getProvider("fiveProvider").get().value, is("fiveProvider"));
    }
}
