package com.flab.ticketing.auth.controller

import com.flab.ticketing.auth.dto.UserInfoResponse
import com.flab.ticketing.auth.dto.request.*
import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.auth.service.AuthService
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.exception.InvalidValueException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class AuthController(
    private val authService: AuthService
) {
    @Operation(
        summary = "이메일 인증 코드 전송",
        description = "제공된 이메일 주소로 인증 코드를 전송합니다",
        responses = [
            ApiResponse(responseCode = "200", description = "이메일이 성공적으로 전송됨"),
            ApiResponse(
                responseCode = "400", description = "잘못된 이메일 형식 - COMMON-001"
            ),
            ApiResponse(
                responseCode = "409", description = "이미 가입된 이메일 - AUTH-001",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/new/email")
    fun emailSend(@Validated @RequestBody emailInfo: UserEmailRegisterRequest) {
        authService.sendEmailVerifyCode(emailInfo.email)
    }


    @Operation(
        summary = "로그인",
        description = "로그인해 성공시 JWT 토큰을 반환합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "로그인 정상 처리"),
            ApiResponse(
                responseCode = "401", description = "이메일 조회 불가 또는 잘못된 비밀번호 - AUTH-008",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/login")
    fun loginMock(@RequestBody userLoginRequest: UserLoginRequest) {

    }

    @Operation(
        summary = "이메일 코드 확인",
        description = "이메일 주소로 전송된 코드를 확인합니다",
        responses = [
            ApiResponse(responseCode = "200", description = "이메일이 성공적으로 확인됨"),
            ApiResponse(
                responseCode = "404", description = "인증 코드를 보낸적 없는 이메일이거나 만료된 인증 코드일 때 - AUTH-003",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "잘못된 인증 코드 - AUTH-004"
            )
        ]
    )
    @PostMapping("/new/email/verify")
    fun verifyEmailCode(@Validated @RequestBody verifyInfo: UserEmailVerificationRequest) {
        authService.verifyEmailCode(verifyInfo.email, verifyInfo.code)
    }

    @Operation(
        summary = "새 사용자 등록",
        description = "확인된 사용자의 정보를 저장합니다",
        responses = [
            ApiResponse(responseCode = "200", description = "사용자가 성공적으로 등록됨"),
            ApiResponse(
                responseCode = "400", description = "영문, 숫자, 특수문자를 1글자씩 포함한 8자 이상의 비밀번호 조건 불만족- COMMON-001"
            ),
            ApiResponse(
                responseCode = "400", description = "이메일 인증 시도를 하지 않았거나 가입 유효시간이 지난 사용자의 경우- AUTH-003"
            ),
            ApiResponse(
                responseCode = "400", description = "이메일 인증 메일은 전송하였으나, 메일 인증을 완료하지 않은 사용자의 경우 - AUTH-005"
            ),
            ApiResponse(
                responseCode = "400", description = "비밀번호와 비밀번호 확인이 불일치 - AUTH-006",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/new/info")
    fun saveVerifiedUserInfo(@Validated @RequestBody registerInfo: UserRegisterRequest) {
        if (!registerInfo.password.equals(registerInfo.passwordConfirm)) {
            throw InvalidValueException(AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
        }

        authService.saveVerifiedUserInfo(registerInfo)
    }

    @Operation(
        summary = "비밀번호 업데이트",
        description = "인증된 사용자의 비밀번호를 업데이트합니다",
        responses = [
            ApiResponse(responseCode = "200", description = "비밀번호가 성공적으로 업데이트됨"),
            ApiResponse(
                responseCode = "400", description = "조건을 만족하지 못한 새 비밀번호 - COMMON-001"
            ),
            ApiResponse(
                responseCode = "400", description = "서로 다른 newPassword와 newPasswordConfirm 입력시 - AUTH-006"
            ),
            ApiResponse(
                responseCode = "400", description = "잘못된 현재 비밀번호 - AUTH-012",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PatchMapping("/password")
    fun updatePassword(
        @LoginUser userInfo: AuthenticatedUserDto,
        @Validated @RequestBody passwordUpdateDto: UserPasswordUpdateRequest
    ) {
        if (!passwordUpdateDto.newPassword.equals(passwordUpdateDto.newPasswordConfirm)) {
            throw InvalidValueException(AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
        }


        authService.updatePassword(userInfo.email, passwordUpdateDto.currentPassword, passwordUpdateDto.newPassword)
    }

    @Operation(
        summary = "사용자 정보 조회",
        description = "인증된 사용자의 정보를 조회합니다",
        responses = [
            ApiResponse(
                responseCode = "200", description = "비밀번호가 성공적으로 업데이트됨",
                content = [Content(schema = Schema(implementation = UserInfoResponse::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "만료된 토큰 - AUTH-010",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "잘못된 토큰 - AUTH-009",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/info")
    fun getUserInfo(
        @LoginUser userInfo: AuthenticatedUserDto
    ): UserInfoResponse {
        return UserInfoResponse.of(userInfo)
    }

}