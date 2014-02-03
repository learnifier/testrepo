/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class TokenAmount {
    private final String name;
    private long totalAmount;

    public TokenAmount(String name) {
        this.name = name;
    }
    
    public long getAmount() {
        return totalAmount;
    }

    public void setAmount(long amount) {
        this.totalAmount = amount;
    }

    public void addAmount(long amount) {
        this.totalAmount+= amount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TokenAmount{" + "totalAmount=" + totalAmount + '}';
    }

}
