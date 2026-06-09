package com.bookcollection.config;

import com.bookcollection.interceptor.JwtInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig 单元测试")
class WebConfigTest {

    @Mock
    private JwtInterceptor jwtInterceptor;

    @Mock
    private CorsRegistry corsRegistry;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private ResourceHandlerRegistry resourceHandlerRegistry;

    @Mock
    private CorsRegistration corsRegistration;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    @Mock
    private ResourceHandlerRegistration resourceHandlerRegistration;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig(jwtInterceptor);

        // lenient 避免 strict stubbing 因 Fluent API 参数不精确匹配而失败
        lenient().when(corsRegistry.addMapping(anyString()))
                .thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedOriginPatterns(anyString()))
                .thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedMethods(any(String[].class)))
                .thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowedHeaders(anyString()))
                .thenReturn(corsRegistration);
        lenient().when(corsRegistration.allowCredentials(anyBoolean()))
                .thenReturn(corsRegistration);
        lenient().when(corsRegistration.maxAge(anyLong()))
                .thenReturn(corsRegistration);

        lenient().when(interceptorRegistry.addInterceptor(any(JwtInterceptor.class)))
                .thenReturn(interceptorRegistration);
        lenient().when(interceptorRegistration.addPathPatterns(anyString()))
                .thenReturn(interceptorRegistration);
        lenient().when(interceptorRegistration.excludePathPatterns(any(String[].class)))
                .thenReturn(interceptorRegistration);

        lenient().when(resourceHandlerRegistry.addResourceHandler(anyString()))
                .thenReturn(resourceHandlerRegistration);
        lenient().when(resourceHandlerRegistration.addResourceLocations(anyString()))
                .thenReturn(resourceHandlerRegistration);
    }

    // ==================== addCorsMappings ====================

    @Nested
    @DisplayName("addCorsMappings 跨域配置")
    class AddCorsMappings {

        @Test
        @DisplayName("配置所有路径的 CORS 规则")
        void shouldConfigureCorsForAllPaths() {
            webConfig.addCorsMappings(corsRegistry);

            verify(corsRegistry).addMapping("/**");
            verify(corsRegistration).allowedOriginPatterns("*");
            verify(corsRegistration).allowedMethods(
                    "GET", "POST", "PUT", "DELETE", "OPTIONS");
            verify(corsRegistration).allowedHeaders("*");
            verify(corsRegistration).allowCredentials(true);
            verify(corsRegistration).maxAge(3600);
        }

        @Test
        @DisplayName("addCorsMappings 不抛异常")
        void shouldNotThrowException() {
            assertDoesNotThrow(() -> webConfig.addCorsMappings(corsRegistry));
        }
    }

    // ==================== addInterceptors ====================

    @Nested
    @DisplayName("addInterceptors 拦截器配置")
    class AddInterceptors {

        @Test
        @DisplayName("添加 JWT 拦截器并对 /api/** 生效")
        void shouldAddJwtInterceptorForApiPaths() {
            webConfig.addInterceptors(interceptorRegistry);

            verify(interceptorRegistry).addInterceptor(jwtInterceptor);
            verify(interceptorRegistration).addPathPatterns("/api/**");
        }

        @Test
        @DisplayName("排除注册/登录/公开文章接口")
        void shouldExcludePublicEndpoints() {
            webConfig.addInterceptors(interceptorRegistry);

            ArgumentCaptor<String[]> captor =
                    ArgumentCaptor.forClass(String[].class);
            verify(interceptorRegistration).excludePathPatterns(captor.capture());
            String[] excluded = captor.getValue();

            assertNotNull(excluded);
            assertEquals(7, excluded.length);
            assertEquals("/api/user/register", excluded[0]);
            assertEquals("/api/user/login", excluded[1]);
            assertEquals("/api/article/hot", excluded[2]);
            assertEquals("/api/article/latest", excluded[3]);
            assertEquals("/api/article/hot/category/*", excluded[4]);
            assertEquals("/api/article/latest/category/*", excluded[5]);
            assertEquals("/api/article/*", excluded[6]);
        }

        @Test
        @DisplayName("addInterceptors 不抛异常")
        void shouldNotThrowException() {
            assertDoesNotThrow(() ->
                    webConfig.addInterceptors(interceptorRegistry));
        }
    }

    // ==================== addResourceHandlers ====================

    @Nested
    @DisplayName("addResourceHandlers 静态资源配置")
    class AddResourceHandlers {

        @Test
        @DisplayName("映射 /uploads/** 到本地文件路径")
        void shouldMapUploadsToLocalFileSystem() {
            webConfig.addResourceHandlers(resourceHandlerRegistry);

            verify(resourceHandlerRegistry).addResourceHandler("/uploads/**");
            verify(resourceHandlerRegistration)
                    .addResourceLocations(contains("file:"));
            verify(resourceHandlerRegistration)
                    .addResourceLocations(contains("uploads"));
        }

        @Test
        @DisplayName("addResourceHandlers 不抛异常")
        void shouldNotThrowException() {
            assertDoesNotThrow(() ->
                    webConfig.addResourceHandlers(resourceHandlerRegistry));
        }
    }
}
