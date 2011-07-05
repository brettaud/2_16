package com.algoTrader.entity;

import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.PositionVO;

public class PositionDaoImpl extends PositionDaoBase {

    public void toPositionVO(Position position, PositionVO positionVO) {

        super.toPositionVO(position, positionVO);

        completePositionVO(position, positionVO);
    }

    public PositionVO toPositionVO(final Position position) {

        PositionVO positionVO = super.toPositionVO(position);

        completePositionVO(position, positionVO);

        return positionVO;
    }

    private void completePositionVO(Position position, PositionVO positionVO) {

        int scale = position.getSecurity().getSecurityFamily().getScale();
        positionVO.setSymbol(position.getSecurity().getSymbol());
        positionVO.setCurrency(position.getSecurity().getSecurityFamily().getCurrency());
        positionVO.setMarketPrice(RoundUtil.getBigDecimal(position.getMarketPriceDouble(), scale));
        positionVO.setMarketValue(RoundUtil.getBigDecimal(position.getMarketValueDouble()));
        positionVO.setAveragePrice(RoundUtil.getBigDecimal(position.getAveragePriceDouble(), scale));
        positionVO.setCost(RoundUtil.getBigDecimal(position.getCostDouble()));
        positionVO.setUnrealizedPL(RoundUtil.getBigDecimal(position.getUnrealizedPLDouble()));
        positionVO.setExitValue(position.getExitValue() != null ? RoundUtil.getBigDecimal(position.getExitValue(), scale) : null);
        positionVO.setRedemptionValue(RoundUtil.getBigDecimal(position.getRedemptionValueDouble()));
        positionVO.setMaxLoss(RoundUtil.getBigDecimal(position.getMaxLossDouble()));

    }

    public Position positionVOToEntity(PositionVO positionVO) {

        throw new UnsupportedOperationException("positionVOToEntity not yet implemented.");
    }
}
