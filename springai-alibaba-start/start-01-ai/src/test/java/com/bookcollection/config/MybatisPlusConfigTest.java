package com.bookcollection.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MybatisPlusConfig 单元测试")
class MybatisPlusConfigTest {

    // ==================== mybatisPlusInterceptor ====================

    @Nested
    @DisplayName("mybatisPlusInterceptor Bean")
    class MybatisPlusInterceptorBean {

        @Test
        @DisplayName("返回非 null 的 MybatisPlusInterceptor")
        void shouldReturnNonNullInterceptor() {
            MybatisPlusConfig config = new MybatisPlusConfig();
            MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

            assertNotNull(interceptor);
        }

        @Test
        @DisplayName("每次调用返回新实例（原型 Bean）")
        void shouldReturnDifferentInstances() {
            MybatisPlusConfig config = new MybatisPlusConfig();

            MybatisPlusInterceptor i1 = config.mybatisPlusInterceptor();
            MybatisPlusInterceptor i2 = config.mybatisPlusInterceptor();

            assertNotNull(i1);
            assertNotNull(i2);
            assertNotSame(i1, i2, "每次调用应返回新的实例");
        }
    }

    // ==================== MyMetaObjectHandler ====================

    @Nested
    @DisplayName("MyMetaObjectHandler 自动填充处理器")
    class MyMetaObjectHandlerTests {

        @Test
        @DisplayName("可以正常实例化")
        void shouldBeInstantiable() {
            MybatisPlusConfig.MyMetaObjectHandler handler =
                    new MybatisPlusConfig.MyMetaObjectHandler();

            assertNotNull(handler);
        }

        @Test
        @DisplayName("是 MetaObjectHandler 的子类")
        void shouldExtendMetaObjectHandler() {
            MybatisPlusConfig.MyMetaObjectHandler handler =
                    new MybatisPlusConfig.MyMetaObjectHandler();

            assertInstanceOf(MetaObjectHandler.class, handler);
        }

        @Test
        @DisplayName("多次实例化返回不同对象")
        void shouldCreateMultipleInstances() {
            MybatisPlusConfig.MyMetaObjectHandler h1 =
                    new MybatisPlusConfig.MyMetaObjectHandler();
            MybatisPlusConfig.MyMetaObjectHandler h2 =
                    new MybatisPlusConfig.MyMetaObjectHandler();

            assertNotNull(h1);
            assertNotNull(h2);
            assertNotSame(h1, h2);
        }
    }
}
