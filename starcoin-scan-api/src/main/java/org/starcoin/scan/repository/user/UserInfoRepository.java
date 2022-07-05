package org.starcoin.scan.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.UserInfo;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    @Query(value = "select *  from {h-domain}user_info where wallet_addr = :wallet_addr"
            , nativeQuery = true)
    UserInfo getByWalletAddr(@Param("wallet_addr") String addr);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}user_info set wallet_addr=:new_wallet_addr where user_id= :user_id and wallet_addr = :old_address", nativeQuery = true)
    void updateAddress(@Param("new_wallet_addr")String newAddress, @Param("user_id")long userId,  @Param("old_address") String old);

    @Modifying
    @Transactional
    @Query(value = "update {h-domain}user_info set is_valid=false where user_id= :user_id and is_valid = true", nativeQuery = true)
    void updateStatus(@Param("user_id")long userId);
}
