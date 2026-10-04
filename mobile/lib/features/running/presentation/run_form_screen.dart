import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/utils/formatters.dart';
import '../../../core/widgets/entity_form_screen.dart';
import '../data/run_repository.dart';
import '../models/run.dart';

/// Creates a run, or edits [run] when given. Pops `true` after saving.
class RunFormScreen extends StatefulWidget {
  const RunFormScreen({super.key, required this.repository, this.run});

  final RunRepository repository;
  final Run? run;

  @override
  State<RunFormScreen> createState() => _RunFormScreenState();
}

class _RunFormScreenState extends State<RunFormScreen> {
  late final TextEditingController _distance;
  late final TextEditingController _duration;
  late final TextEditingController _calories;
  late final TextEditingController _notes;
  late DateTime _runAt;

  @override
  void initState() {
    super.initState();
    final run = widget.run;
    _runAt = run?.runAt ?? DateTime.now();
    _distance = TextEditingController(text: run == null ? '' : formatDecimal(run.distanceKm, fractionDigits: 2));
    _duration = TextEditingController(text: run?.durationMinutes.toString() ?? '');
    _calories = TextEditingController(text: run?.caloriesBurned.toString() ?? '');
    _notes = TextEditingController(text: run?.notes ?? '');
  }

  @override
  void dispose() {
    _distance.dispose();
    _duration.dispose();
    _calories.dispose();
    _notes.dispose();
    super.dispose();
  }

  static double? _parseDecimal(String text) => double.tryParse(text.trim().replaceAll(',', '.'));

  Future<void> _pickRunAt() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: _runAt.isAfter(now) ? now : _runAt,
      firstDate: DateTime(now.year - 5),
      lastDate: now,
    );
    if (date == null || !mounted) return;

    final time = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(_runAt));
    if (!mounted) return;
    final picked = DateTime(date.year, date.month, date.day, time?.hour ?? _runAt.hour, time?.minute ?? _runAt.minute);
    setState(() => _runAt = picked.isAfter(now) ? now : picked);
  }

  Future<void> _save() {
    final notes = _notes.text.trim();
    final calories = _calories.text.trim();
    final input = RunInput(
      distanceKm: _parseDecimal(_distance.text)!,
      durationMinutes: int.parse(_duration.text.trim()),
      caloriesBurned: calories.isEmpty ? null : int.parse(calories),
      notes: notes.isEmpty ? null : notes,
      runAt: _runAt,
    );
    final run = widget.run;
    return run == null ? widget.repository.createRun(input) : widget.repository.updateRun(run.id, input);
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.run != null;
    return EntityFormScreen(
      title: isEditing ? 'Koşuyu düzenle' : 'Yeni koşu',
      submitLabel: isEditing ? 'Kaydet' : 'Ekle',
      onSubmit: _save,
      fieldsBuilder: (context, serverErrors) => [
        InkWell(
          onTap: _pickRunAt,
          borderRadius: BorderRadius.circular(4),
          child: InputDecorator(
            decoration: InputDecoration(
              labelText: 'Tarih ve saat',
              prefixIcon: const Icon(Icons.event),
              suffixIcon: const Icon(Icons.edit_calendar_outlined),
              border: const OutlineInputBorder(),
              errorText: serverErrors['runAt'],
            ),
            child: Text(formatDateTime(_runAt)),
          ),
        ),
        TextFormField(
          controller: _distance,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
          textInputAction: TextInputAction.next,
          decoration: InputDecoration(
            labelText: 'Mesafe',
            suffixText: 'km',
            prefixIcon: const Icon(Icons.straighten),
            border: const OutlineInputBorder(),
            errorText: serverErrors['distanceKm'],
          ),
          validator: (value) {
            final parsed = _parseDecimal(value ?? '');
            if (parsed == null) return 'Geçerli bir mesafe giriniz';
            if (parsed < 0.1) return 'Mesafe en az 0,1 km olmalıdır';
            return null;
          },
        ),
        _IntegerField(
          controller: _duration,
          label: 'Süre',
          suffix: 'dk',
          icon: Icons.timer_outlined,
          serverError: serverErrors['durationMinutes'],
          emptyMessage: 'Süre zorunludur',
        ),
        _IntegerField(
          controller: _calories,
          label: 'Kalori',
          suffix: 'kcal',
          icon: Icons.local_fire_department_outlined,
          serverError: serverErrors['caloriesBurned'],
        ),
        TextFormField(
          controller: _notes,
          maxLength: 500,
          maxLines: 3,
          decoration: InputDecoration(
            labelText: 'Not (opsiyonel)',
            alignLabelWithHint: true,
            border: const OutlineInputBorder(),
            errorText: serverErrors['notes'],
          ),
        ),
      ],
    );
  }
}

class _IntegerField extends StatelessWidget {
  const _IntegerField({
    required this.controller,
    required this.label,
    required this.suffix,
    required this.icon,
    this.emptyMessage,
    this.serverError,
  });

  final TextEditingController controller;
  final String label;
  final String suffix;
  final IconData icon;
  /// Required when set; otherwise the field is optional.
  final String? emptyMessage;
  final String? serverError;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      textInputAction: TextInputAction.next,
      decoration: InputDecoration(
        labelText: emptyMessage == null ? '$label (opsiyonel)' : label,
        suffixText: suffix,
        prefixIcon: Icon(icon),
        border: const OutlineInputBorder(),
        errorText: serverError,
      ),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (text.isEmpty) return emptyMessage;
        final parsed = int.tryParse(text);
        if (parsed == null) return 'Geçerli bir sayı giriniz';
        if (parsed < 1) return '$label en az 1 olmalıdır';
        return null;
      },
    );
  }
}
