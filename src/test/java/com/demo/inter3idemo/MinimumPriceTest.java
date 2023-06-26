package com.demo.inter3idemo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MinimumPriceTest {

    private MinimumPrice minimumPriceUnderTest;

    @BeforeEach
    void setUp() {
        minimumPriceUnderTest = new MinimumPrice();
    }

    @Test
    void testDemoTest() {
        // Setup
        final GoodsDO goodsDO = new GoodsDO();
        goodsDO.setGoodsPrice(new BigDecimal("100.00"));
        goodsDO.setGoodsNum(new BigDecimal("1.00"));

        final PromotionDO promotionDO = new PromotionDO();
        promotionDO.setPromotionPrice(new BigDecimal("150.00"));
        promotionDO.setFavouredAmount(new BigDecimal("40.00"));
        promotionDO.setPromotionNum(new BigDecimal("1.00"));
        promotionDO.setPromotionType(1);
        promotionDO.setCalculateType(1);

        // Run the test
        final BigDecimal result = minimumPriceUnderTest.demoTest(goodsDO, promotionDO);

        // Verify the results
        assertThat(result).isEqualTo(new BigDecimal("0.00"));
    }
}
