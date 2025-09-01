package tw.petpick.petpick.auth;

public record VerifyEmailRequest(
    String email,
    String code
) {}