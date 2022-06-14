package org.starcoin.scan.entity;

import org.starcoin.bean.AddressHolder;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "address_holder")
public class AddressHolderEntity extends AddressHolder {
}
