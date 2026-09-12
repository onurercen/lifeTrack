class AuthResponse {
  const AuthResponse({required this.token, required this.user});

  final String token;
  final AuthUser user;

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'] as String,
      user: AuthUser.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}

class AuthUser {
  const AuthUser({this.id, required this.name, required this.email});

  final int? id;
  final String name;
  final String email;

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: json['id'] as int?,
      name: json['name'] as String,
      email: json['email'] as String,
    );
  }
}
