package org.starcoin.indexer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.starcoin.bean.AddressHolder;
import org.starcoin.bean.TokenMarketCap;
import org.starcoin.indexer.repository.AddressHolderRepository;
import org.starcoin.indexer.repository.MarketCapDTO;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressHolderService {
    @Autowired
    private AddressHolderRepository addressHolderRepository;

    public void saveList(List<AddressHolder> addressHolderList) {
        if(addressHolderList != null && !addressHolderList.isEmpty()) {
            addressHolderRepository.saveAllAndFlush(addressHolderList);
        }
    }

    public void save(AddressHolder addressHolder) {
        addressHolderRepository.upsert(addressHolder.getAddress(), addressHolder.getToken(), addressHolder.getAmount(), addressHolder.getUpdateTime());
    }

    public void delete(AddressHolder addressHolder) {
        if(addressHolder != null) {
            addressHolderRepository.deleteByAddressAndToken(addressHolder.getAddress(), addressHolder.getToken());
        }
    }

    public List<TokenMarketCap> getMarketCap() {
        List<TokenMarketCap> result = new ArrayList<>();
        List<MarketCapDTO> marketCapDTOList = addressHolderRepository.getMarketCap();
        if(marketCapDTOList != null) {
            for (MarketCapDTO dto: marketCapDTOList) {
                result.add(new TokenMarketCap(dto.getToken(), dto.getMarket()));
            }
        }
        return result;
    }
}
