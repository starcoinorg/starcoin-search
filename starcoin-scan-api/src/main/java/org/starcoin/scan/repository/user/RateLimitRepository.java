package org.starcoin.scan.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.starcoin.bean.RateLimit;

public interface RateLimitRepository extends JpaRepository<RateLimit, String> {
}
