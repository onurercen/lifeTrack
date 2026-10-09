import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/network/api_exception.dart';
import 'auth_widgets.dart';

/// Asks for the e-mail address, then for the e-mailed code and a new password.
/// A successful reset signs the user in.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, this.initialEmail = ''});

  final String initialEmail;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  late final _email = TextEditingController(text: widget.initialEmail.trim());
  final _code = TextEditingController();
  final _password = TextEditingController();
  final _confirmation = TextEditingController();
  bool _codeSent = false;
  bool _busy = false;
  Map<String, String> _serverErrors = const {};

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    _confirmation.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    setState(() => _serverErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    await _run(() async {
      await AppScope.read(context).auth.forgotPassword(_email.text.trim());
      if (mounted) setState(() => _codeSent = true);
    });
  }

  Future<void> _resendCode() async {
    await _run(() async {
      await AppScope.read(context).auth.forgotPassword(_email.text.trim());
      _showMessage('Kayıtlı bir hesap varsa yeni kod gönderildi.');
    });
  }

  Future<void> _reset() async {
    setState(() => _serverErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    await _run(() => AppScope.read(context).auth.resetPassword(
          email: _email.text.trim(),
          code: _code.text,
          newPassword: _password.text,
        ));
    // On success the app switches to the home screen and closes this one.
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _busy = true);
    try {
      await action();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _serverErrors = e.fieldErrors);
      if (e.fieldErrors.isEmpty) _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _showMessage(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  void _changeEmail() {
    setState(() {
      _codeSent = false;
      _serverErrors = const {};
      _code.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Şifremi unuttum')),
      body: AuthFormBody(
        formKey: _formKey,
        children: _codeSent ? _resetStep(theme) : _emailStep(theme),
      ),
    );
  }

  List<Widget> _emailStep(ThemeData theme) => [
        Text(
          'Kayıtlı e-posta adresini gir, şifreni sıfırlamak için bir kod gönderelim.',
          style: theme.textTheme.bodyLarge,
        ),
        const SizedBox(height: 24),
        TextFormField(
          controller: _email,
          keyboardType: TextInputType.emailAddress,
          autofillHints: const [AutofillHints.email],
          textInputAction: TextInputAction.done,
          onFieldSubmitted: (_) => _sendCode(),
          decoration: InputDecoration(
            labelText: 'E-posta',
            prefixIcon: const Icon(Icons.email_outlined),
            border: const OutlineInputBorder(),
            errorText: _serverErrors['email'],
          ),
          validator: (value) {
            final text = value?.trim() ?? '';
            if (text.isEmpty) return 'E-posta zorunludur';
            if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)) return 'Geçerli bir e-posta giriniz';
            return null;
          },
        ),
        const SizedBox(height: 24),
        BusyButton(label: 'Kod gönder', busy: _busy, onPressed: _sendCode),
      ];

  List<Widget> _resetStep(ThemeData theme) => [
        Text(
          '${_email.text.trim()} adresine kayıtlı bir hesap varsa 6 haneli bir kod gönderdik. '
          'Kodu ve yeni şifreni gir.',
          style: theme.textTheme.bodyLarge,
        ),
        const SizedBox(height: 24),
        CodeField(controller: _code, serverError: _serverErrors['code']),
        const SizedBox(height: 16),
        TextFormField(
          controller: _password,
          obscureText: true,
          autofillHints: const [AutofillHints.newPassword],
          textInputAction: TextInputAction.next,
          decoration: InputDecoration(
            labelText: 'Yeni şifre',
            prefixIcon: const Icon(Icons.lock_reset),
            border: const OutlineInputBorder(),
            errorText: _serverErrors['newPassword'],
          ),
          validator: (value) {
            if (value == null || value.isEmpty) return 'Yeni şifre zorunludur';
            if (value.length < 6) return 'Şifre en az 6 karakter olmalıdır';
            return null;
          },
        ),
        const SizedBox(height: 16),
        TextFormField(
          controller: _confirmation,
          obscureText: true,
          textInputAction: TextInputAction.done,
          onFieldSubmitted: (_) => _reset(),
          decoration: const InputDecoration(
            labelText: 'Yeni şifre (tekrar)',
            prefixIcon: Icon(Icons.lock_reset),
            border: OutlineInputBorder(),
          ),
          validator: (value) => value != _password.text ? 'Şifreler eşleşmiyor' : null,
        ),
        const SizedBox(height: 8),
        Text('Şifren değişince tüm cihazlardaki oturumların kapanır.', style: theme.textTheme.bodySmall),
        const SizedBox(height: 24),
        BusyButton(label: 'Şifreyi değiştir', busy: _busy, onPressed: _reset),
        const SizedBox(height: 12),
        Wrap(
          alignment: WrapAlignment.spaceBetween,
          children: [
            TextButton(onPressed: _busy ? null : _changeEmail, child: const Text('E-postayı değiştir')),
            TextButton(onPressed: _busy ? null : _resendCode, child: const Text('Kodu tekrar gönder')),
          ],
        ),
      ];
}
