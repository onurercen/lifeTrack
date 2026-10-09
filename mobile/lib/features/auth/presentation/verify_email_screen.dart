import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/network/api_exception.dart';
import 'auth_widgets.dart';

/// Shown instead of the app until the user enters the code sent to their address.
class VerifyEmailScreen extends StatefulWidget {
  const VerifyEmailScreen({super.key});

  @override
  State<VerifyEmailScreen> createState() => _VerifyEmailScreenState();
}

class _VerifyEmailScreenState extends State<VerifyEmailScreen> {
  final _formKey = GlobalKey<FormState>();
  final _code = TextEditingController();
  String? _codeError;
  bool _submitting = false;
  bool _resending = false;

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _codeError = null);
    if (!_formKey.currentState!.validate()) return;

    setState(() => _submitting = true);
    try {
      await AppScope.read(context).auth.verifyEmail(_code.text);
      // AuthGate shows the app once the user is verified.
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _codeError = e.fieldErrors['code']);
      if (_codeError == null) _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _resend() async {
    setState(() => _resending = true);
    try {
      await AppScope.read(context).auth.resendVerificationCode();
      _showMessage('Yeni kod gönderildi.');
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  void _showMessage(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final auth = AppScope.of(context).auth;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('E-postanı doğrula'),
        actions: [TextButton(onPressed: auth.logout, child: const Text('Çıkış yap'))],
      ),
      body: AuthFormBody(
        formKey: _formKey,
        children: [
          Icon(Icons.mark_email_unread_outlined, size: 56, color: theme.colorScheme.primary),
          const SizedBox(height: 20),
          Text(
            'Kodu e-postana gönderdik',
            style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            '${auth.user?.email ?? 'E-posta'} adresine gelen 6 haneli kodu gir. '
            'Birkaç dakika içinde gelmezse spam klasörüne bak.',
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 28),
          CodeField(controller: _code, serverError: _codeError, onSubmitted: _submit),
          const SizedBox(height: 24),
          BusyButton(label: 'Doğrula', busy: _submitting, onPressed: _submit),
          const SizedBox(height: 12),
          TextButton(
            onPressed: _resending ? null : _resend,
            child: const Text('Kodu tekrar gönder'),
          ),
        ],
      ),
    );
  }
}
