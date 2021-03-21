package com.rakiwow.fitsynckoala.retrofit

import com.rakiwow.fitsynckoala.model.RefreshTokenResult
import com.rakiwow.fitsynckoala.util.EntityMapper
import javax.inject.Inject

class RefreshTokenMapper
@Inject constructor() : EntityMapper<RefreshTokenResultEntity, RefreshTokenResult> {

    override fun mapFromEntity(entity: RefreshTokenResultEntity): RefreshTokenResult {
        return RefreshTokenResult(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            tokenType = entity.tokenType,
            expiresAt = entity.expiresAt,
            expiresIn = entity.expiresIn
        )
    }

    override fun mapToEntity(domainModel: RefreshTokenResult): RefreshTokenResultEntity {
        return RefreshTokenResultEntity(
            accessToken = domainModel.accessToken,
            refreshToken = domainModel.refreshToken,
            tokenType = domainModel.tokenType,
            expiresAt = domainModel.expiresAt,
            expiresIn = domainModel.expiresIn
        )
    }
}