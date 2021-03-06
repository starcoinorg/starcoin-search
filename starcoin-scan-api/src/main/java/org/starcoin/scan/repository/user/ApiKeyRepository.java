package org.starcoin.scan.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.ApiKey;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    @Query(value = "select *  from {h-domain}api_keys where user_id = :user_id and is_valid = true"
            , nativeQuery = true)
    List<ApiKey> findApiKeyByUserId(@Param("user_id") long userId);
    @Query(value = "select *  from {h-domain}api_keys where user_id = :user_id and app_name=:app_name"
            , nativeQuery = true)
    ApiKey getApiKeyByNameAndUserId(@Param("user_id") long userId, @Param("app_name")String appName);

    @Modifying
    @Transactional
    @Query(value = "delete from {h-domain}api_keys where  user_id = :user_id", nativeQuery = true)
    int deleteAllByUserId(@Param("user_id") long userId);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}api_keys set app_name=:app_name where api_key= :api_key ", nativeQuery = true)
    int updateAppName(@Param("app_name")String appName, @Param("api_key") String apiKey);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}api_keys set is_valid=false where api_key= :api_key and is_valid = true", nativeQuery = true)
    int deleteByAppKey(@Param("api_key") String apiKey);
}
