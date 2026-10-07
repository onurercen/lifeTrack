import 'package:flutter/material.dart';

import '../network/api_exception.dart';

/// Shared add/edit form scaffold. Runs [onSubmit] after local validation,
/// shows server field errors through [fieldsBuilder] and pops `true` on success.
class EntityFormScreen extends StatefulWidget {
  const EntityFormScreen({
    super.key,
    required this.title,
    required this.submitLabel,
    required this.fieldsBuilder,
    required this.onSubmit,
  });

  final String title;
  final String submitLabel;
  final List<Widget> Function(BuildContext context, Map<String, String> serverErrors) fieldsBuilder;
  final Future<void> Function() onSubmit;

  @override
  State<EntityFormScreen> createState() => _EntityFormScreenState();
}

class _EntityFormScreenState extends State<EntityFormScreen> {
  final _formKey = GlobalKey<FormState>();
  Map<String, String> _serverErrors = const {};
  bool _isSubmitting = false;

  Future<void> _submit() async {
    setState(() => _serverErrors = const {});
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);
    try {
      await widget.onSubmit();
      // Skip when the submit already navigated away (e.g. signing out after deleting the account).
      if (mounted && (ModalRoute.of(context)?.isCurrent ?? false)) Navigator.pop(context, true);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _serverErrors = e.fieldErrors);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final fields = widget.fieldsBuilder(context, _serverErrors);
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              for (final field in fields) ...[field, const SizedBox(height: 16)],
              const SizedBox(height: 8),
              FilledButton(
                onPressed: _isSubmitting ? null : _submit,
                child: _isSubmitting
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : Text(widget.submitLabel),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Outlined text field; required when [requiredMessage] is given.
class FormTextField extends StatelessWidget {
  const FormTextField({
    super.key,
    required this.controller,
    required this.label,
    this.requiredMessage,
    this.icon,
    this.serverError,
    this.maxLength,
    this.maxLines = 1,
    this.keyboardType,
    this.hintText,
    this.validator,
    this.obscureText = false,
  });

  final TextEditingController controller;
  final String label;
  final String? requiredMessage;
  final IconData? icon;
  final String? serverError;
  final int? maxLength;
  final int maxLines;
  final TextInputType? keyboardType;
  final String? hintText;
  final bool obscureText;

  /// Extra check for non-empty input.
  final String? Function(String text)? validator;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      obscureText: obscureText,
      autocorrect: !obscureText,
      enableSuggestions: !obscureText,
      maxLines: obscureText ? 1 : maxLines,
      maxLength: maxLength,
      keyboardType: keyboardType,
      textInputAction: maxLines > 1 ? TextInputAction.newline : TextInputAction.next,
      decoration: InputDecoration(
        labelText: requiredMessage == null ? '$label (opsiyonel)' : label,
        hintText: hintText,
        prefixIcon: icon == null ? null : Icon(icon),
        alignLabelWithHint: maxLines > 1,
        border: const OutlineInputBorder(),
        errorText: serverError,
      ),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (text.isEmpty) return requiredMessage;
        return validator?.call(text);
      },
    );
  }
}
