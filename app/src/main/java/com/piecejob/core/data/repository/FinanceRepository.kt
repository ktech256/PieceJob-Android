package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class FinanceRepository @Inject constructor(
    private val api: PieceJobApi
) {
    // Finance specific methods can go here
    // Currently most finance info for providers is in Wallet and Payout repositories
}
