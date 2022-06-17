package org.starcoin.indexer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.starcoin.bean.AddressHolderEntity;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;


//TODO merge to commons repository dir
public interface AddressHolderRepository extends JpaRepository<AddressHolderEntity, String> {
    @Modifying
    @Transactional
    @Query(value = "delete from  {h-domain}address_holder where address=:address and token=:token ", nativeQuery = true)
    void deleteByAddressAndToken(@Param("address")String address, @Param("token")String token);
    @Modifying
    @Transactional
    @Query(value = "insert into {h-domain}address_holder (address, token, amount, update_time) values (:address, :token, :amount, :update_time) on conflict(address, token) do update set amount=:amount, update_time=:update_time ", nativeQuery = true)
    void upsert(@Param("address")String address, @Param("token")String token, @Param("amount")BigInteger amount, @Param("update_time") Date updateTime);

    @Query(value = "select sum(amount) as market, token from {h-domain}address_holder group by token ", nativeQuery = true)
    List<MarketCapDTO> getMarketCap();
}
