class AuthResponse {
  const AuthResponse({required this.token, required this.refreshToken, required this.user});

  /// Short-lived access token.
  final String token;

  /// Exchanged for a new token pair when [token] expires.
  final String refreshToken;
  final AuthUser user;

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'] as String,
      refreshToken: json['refreshToken'] as String,
      user: AuthUser.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}

class AuthUser {
  const AuthUser({this.id, required this.name, required this.email, this.emailVerified = true});

  final int? id;
  final String name;
  final String email;

  /// False until the user enters the code sent to [email]; the app shows the
  /// verification screen instead of their data until then.
  final bool emailVerified;

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: json['id'] as int?,
      name: json['name'] as String,
      email: json['email'] as String,
      emailVerified: json['emailVerified'] as bool? ?? true,
    );
  }
}
