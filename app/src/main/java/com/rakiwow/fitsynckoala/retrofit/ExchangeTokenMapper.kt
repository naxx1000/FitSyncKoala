package com.rakiwow.fitsynckoala.retrofit

import com.rakiwow.fitsynckoala.model.ExchangeTokenResult
import com.rakiwow.fitsynckoala.util.EntityMapper
import javax.inject.Inject

class ExchangeTokenMapper
@Inject constructor() : EntityMapper<ExchangeTokenResultEntity, ExchangeTokenResult>{

    override fun mapFromEntity(entity: ExchangeTokenResultEntity): ExchangeTokenResult {
        return ExchangeTokenResult(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            tokenType = entity.tokenType,
            expiresAt = entity.expiresAt,
            expiresIn = entity.expiresIn,
            athlete = entity.athele
        )
    }

    override fun mapToEntity(domainModel: ExchangeTokenResult): ExchangeTokenResultEntity {
        return ExchangeTokenResultEntity(
            accessToken = domainModel.accessToken,
            refreshToken = domainModel.refreshToken,
            tokenType = domainModel.tokenType,
            expiresAt = domainModel.expiresAt,
            expiresIn = domainModel.expiresIn,
            athele = domainModel.athlete
        )
    }
}