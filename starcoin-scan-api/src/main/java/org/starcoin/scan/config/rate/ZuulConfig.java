package org.starcoin.scan.config.rate;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.map.IMap;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimitKeyGenerator;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimitUtils;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.support.DefaultRateLimitKeyGenerator;
import io.github.bucket4j.grid.GridBucketState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.starcoin.utils.IpUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

@Configuration
public class ZuulConfig {
    private static final String APIKEY = "apikey";
    @Bean
    @Qualifier("RateLimit")
    public IMap<String, GridBucketState> map() {
        return Hazelcast.newHazelcastInstance().getMap("rateLimit");
    }

    @Bean
    public RouteLocator routeLocator() {
        return new RouteLocator() {
            @Override
            public Collection<String> getIgnoredPaths() {
                return null;
            }

            @Override
            public List<Route> getRoutes() {
                return null;
            }

            @Override
            public Route getMatchingRoute(String path) {
                return null;
            }
        };
    }
    @Bean
    public RateLimitKeyGenerator ratelimitKeyGenerator(RateLimitProperties properties, RateLimitUtils rateLimitUtils) {
        return new DefaultRateLimitKeyGenerator(properties, rateLimitUtils) {
            @Override
            public String key(HttpServletRequest request, Route route, RateLimitProperties.Policy policy) {
                String ip = IpUtils.getIpAddr(request);
                String apikey = request.getParameter(APIKEY);
                return apikey + ":" + ip;
            }
        };
    }
}
