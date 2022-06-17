package org.starcoin.scan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.starcoin.bean.TransferJournalEntity;

import java.util.List;

public interface TransferJournalRepository extends JpaRepository<TransferJournalEntity, String> {
    @Query(value = "select sum(amount) as volume, token  from {h-domain}transfer_journal where token = :token and create_time > now() - interval '90 day' group by token "
            , nativeQuery = true)
    TokenVolumeDTO getVolumeByToken(@Param("token") String token);
    @Query(value = "select sum(amount) as volume , token  from {h-domain}transfer_journal where token = :token and create_time = :date"
            , nativeQuery = true)
    TokenVolumeDTO getVolumeByTokenAndDate(@Param("token") String token, @Param("date") String date);

    @Query(value = "select sum(amount) as volume , token  from {h-domain}transfer_journal where create_time > now() - interval '1 day' and amount > 0 group by token "
            , nativeQuery = true)
    List<TokenVolumeDTO> getAllVolumes();
}
