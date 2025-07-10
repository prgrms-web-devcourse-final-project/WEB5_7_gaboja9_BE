package io.gaboja9.mockstock.domain.payments.dto;

import lombok.Getter;

@Getter
public class KakaoPayApproveResponse {
    private String aid;
    private String tid;
    private String cid;
    private String sid;
    private String partner_order_id;
    private String partner_user_id;
    private String payment_method_type;
    private String status;
    private AmountDetail amount;
    private CardInfo card_info;
    private String item_name;
    private String item_code;
    private int quantity;
    private String created_at;
    private String approved_at;
    private String payload;

    public static class AmountDetail {
        private int total;
        private int tax_free;
        private int vat;
        private int point;
        private int discount;
        private int green_deposit;
    }

    public static class CardInfo {
        private String kakaopay_purchase_corp;
        private String kakaopay_purchase_corp_code;
        private String kakaopay_issuer_corp;
        private String kakaopay_issuer_corp_code;
        private String bin;
        private String card_type;
        private String install_month;
        private String approved_id;
        private String card_mid;
        private String interest_free_install;
        private String installment_type;
        private String card_item_code;
    }
}
