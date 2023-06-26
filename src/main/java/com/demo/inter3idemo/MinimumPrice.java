package com.demo.inter3idemo;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author erwinfu
 * @description 计算最低价
 * @date 2023/6/26 12:27
 */
public class MinimumPrice {


    BigDecimal demoTest(GoodsDO goodsDO, PromotionDO promotionDO) {
        CalculateMinimumPriceTemplate calculateMinimumPriceTemplate = new DefaultCalculateMinimumPrice();
        return calculateMinimumPriceTemplate.doCalculate(goodsDO, promotionDO);
    }

}

/**
 * 计算模式
 */
interface Calculate {
    void calculate(GoodsDO goodsDO, PromotionDO promotionDO);
}

/**
 * 求最低价模式
 */
class MinimumPriceCalculate implements Calculate {

    @Override
    public void calculate(GoodsDO goodsDO, PromotionDO promotionDO) {
        BigDecimal promotionPrice = promotionDO.getPromotionPrice();
        BigDecimal goodsPrice = goodsDO.getGoodsPrice();
        if (promotionPrice.compareTo(goodsPrice) > 0) {
            goodsDO.setGoodsNum(
                    promotionPrice.divide(goodsPrice,0, RoundingMode.DOWN)
                            .abs()
                            .add(BigDecimal.ONE));
        }
    }
}
/**
 * 其它计算模式
 */


/**
 * 计算模式适配器
 */
class CalculateAdapter {

    public void calculate(GoodsDO goodsDO, PromotionDO promotionDO) {
        Integer calculateType = promotionDO.getCalculateType();
        // 求最低价模式
        if (calculateType.equals(1)) {
            new MinimumPriceCalculate().calculate(goodsDO, promotionDO);
        }
        new MinimumPriceCalculate().calculate(goodsDO, promotionDO);
    }
}

/**
 * 促销算法
 */
interface PromotionArithmetic {
    BigDecimal arithmetic(GoodsDO goodsDO, PromotionDO promotionDO);
}

/**
 * 满减算法
 */
class FullReductionArithmetic implements PromotionArithmetic {

    /**
     * 满减算法,满足条件则返回优惠后的价格，否则返回原价
     *
     * @param goodsDO
     * @param promotionDO
     * @return
     */
    @Override
    public BigDecimal arithmetic(GoodsDO goodsDO, PromotionDO promotionDO) {
        BigDecimal goodsPrice = goodsDO.getGoodsPrice();
        BigDecimal goodsNum = goodsDO.getGoodsNum();
        BigDecimal promotionPrice = promotionDO.getPromotionPrice();
        BigDecimal favouredAmount = promotionDO.getFavouredAmount();
        BigDecimal multiplied = goodsPrice.multiply(goodsNum);
        // 满减算法,满足条件则返回优惠后的价格，否则返回原价
        if (multiplied.subtract(promotionPrice).compareTo(BigDecimal.ZERO) >= 0) {
            return multiplied.subtract(favouredAmount)
                    .divide(goodsNum, 3, RoundingMode.HALF_UP);
        }
        return goodsDO.getGoodsPrice();
    }
}

/**
 * 其它算法
 */


/**
 * 促销算法适配器
 */
class PromotionArithmeticAdapter {

    public BigDecimal arithmetic(GoodsDO goodsDO, PromotionDO promotionDO) {
        Integer promotionType = promotionDO.getPromotionType();
        if (promotionType.equals(1)) {
            return new FullReductionArithmetic().arithmetic(goodsDO, promotionDO);
        }
        return new FullReductionArithmetic().arithmetic(goodsDO, promotionDO);
    }
}

abstract class CalculateMinimumPriceTemplate {
    abstract void check(GoodsDO goodsDO, PromotionDO promotionDO);

    abstract void calculate(GoodsDO goodsDO, PromotionDO promotionDO);

    abstract BigDecimal arithmetic(GoodsDO goodsDO, PromotionDO promotionDO);

    //模板
    public final BigDecimal doCalculate(GoodsDO goodsDO, PromotionDO promotionDO) {

        // 检查参数
        check(goodsDO, promotionDO);

        // 按计算模式计算数据
        calculate(goodsDO, promotionDO);

        // 求最低价
        return arithmetic(goodsDO, promotionDO);

    }
}

class DefaultCalculateMinimumPrice extends CalculateMinimumPriceTemplate {

    @Override
    void check(GoodsDO goodsDO, PromotionDO promotionDO) {
        // TODO 参数检测
    }

    @Override
    void calculate(GoodsDO goodsDO, PromotionDO promotionDO) {
        new CalculateAdapter().calculate(goodsDO, promotionDO);
    }

    @Override
    BigDecimal arithmetic(GoodsDO goodsDO, PromotionDO promotionDO) {
        return new PromotionArithmeticAdapter().arithmetic(goodsDO, promotionDO);
    }
}


/**
 * 促销do
 */
@Data
class PromotionDO {
    /**
     * 起步价
     */
    private BigDecimal promotionPrice;
    /**
     * 优惠金额
     */
    private BigDecimal favouredAmount;

    /**
     * 起步数量
     */
    private BigDecimal promotionNum;
    /**
     * 促销类型 （此处不做细节的领域拆分了）
     */
    private Integer promotionType;
    /**
     * 计算规则
     */
    private Integer calculateType;

}

/**
 * 商品信息
 */
@Data
class GoodsDO {
    /**
     * 商品价格
     */
    private BigDecimal goodsPrice;
    /**
     * 商品数量
     */
    private BigDecimal goodsNum;

}

