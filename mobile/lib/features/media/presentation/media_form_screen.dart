import 'package:flutter/material.dart';

import '../../../core/widgets/entity_form_screen.dart';
import '../../../core/widgets/form_fields.dart';
import '../data/media_repository.dart';
import '../models/media.dart';

/// Creates a media entry, or edits [media] when given. Pops `true` after saving.
class MediaFormScreen extends StatefulWidget {
  const MediaFormScreen({super.key, required this.repository, this.media});

  final MediaRepository repository;
  final Media? media;

  @override
  State<MediaFormScreen> createState() => _MediaFormScreenState();
}

class _MediaFormScreenState extends State<MediaFormScreen> {
  late final TextEditingController _title = TextEditingController(text: widget.media?.title);
  late final TextEditingController _url = TextEditingController(text: widget.media?.url);
  late final TextEditingController _description = TextEditingController(text: widget.media?.description);
  late String? _type = widget.media?.type;
  late MediaStatus _status = widget.media?.status ?? MediaStatus.planned;
  late int? _rating = widget.media?.rating;
  late DateTime? _finishedOn = widget.media?.finishedOn;

  // Keep a legacy type (e.g. "movie") selectable when editing.
  late final List<String> _typeOptions = [
    ...Media.types,
    if (_type != null && !Media.types.contains(_type)) _type!,
  ];

  static String? _trimToNull(String text) => text.trim().isEmpty ? null : text.trim();

  @override
  void dispose() {
    _title.dispose();
    _url.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _save() {
    final input = MediaInput(
      title: _title.text.trim(),
      type: _type!,
      url: _trimToNull(_url.text),
      description: _trimToNull(_description.text),
      status: _status,
      rating: _rating,
      // The server fills in today when an entry is marked completed without a date.
      finishedOn: _status == MediaStatus.completed ? _finishedOn : null,
    );
    final media = widget.media;
    return media == null ? widget.repository.createMedia(input) : widget.repository.updateMedia(media.id, input);
  }

  static String? _validateUrl(String text) {
    final uri = Uri.tryParse(text);
    if (uri == null || !(uri.scheme == 'http' || uri.scheme == 'https') || uri.host.isEmpty) {
      return 'Geçerli bir bağlantı giriniz (https://...)';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.media != null;
    return EntityFormScreen(
      title: isEditing ? 'Medyayı düzenle' : 'Yeni medya',
      submitLabel: isEditing ? 'Kaydet' : 'Ekle',
      onSubmit: _save,
      fieldsBuilder: (context, serverErrors) => [
        SegmentedButton<MediaStatus>(
          segments: [
            for (final status in MediaStatus.values) ButtonSegment(value: status, label: Text(status.label)),
          ],
          selected: {_status},
          showSelectedIcon: false,
          onSelectionChanged: (selection) => setState(() => _status = selection.single),
        ),
        FormTextField(
          controller: _title,
          label: 'Başlık',
          requiredMessage: 'Medya başlığı zorunludur',
          icon: Icons.title,
          serverError: serverErrors['title'],
        ),
        DropdownButtonFormField<String>(
          initialValue: _type,
          items: [for (final t in _typeOptions) DropdownMenuItem(value: t, child: Text(t))],
          onChanged: (value) => setState(() => _type = value),
          decoration: InputDecoration(
            labelText: 'Tür',
            prefixIcon: const Icon(Icons.category_outlined),
            border: const OutlineInputBorder(),
            errorText: serverErrors['type'],
          ),
          validator: (value) => value == null ? 'Tür zorunludur' : null,
        ),
        if (_status == MediaStatus.completed) ...[
          FormDateField(
            label: 'İzlenme tarihi',
            value: _finishedOn,
            emptyText: widget.media?.status != MediaStatus.completed ? 'Bugün' : 'Seçilmedi',
            icon: Icons.flag_outlined,
            serverError: serverErrors['finishedOn'],
            onChanged: (date) => setState(() => _finishedOn = date),
          ),
          RatingField(
            value: _rating,
            serverError: serverErrors['rating'],
            onChanged: (rating) => setState(() => _rating = rating),
          ),
        ],
        FormTextField(
          controller: _url,
          label: 'Bağlantı',
          hintText: 'https://',
          icon: Icons.link,
          keyboardType: TextInputType.url,
          serverError: serverErrors['url'],
          validator: _validateUrl,
        ),
        FormTextField(
          controller: _description,
          label: 'Açıklama',
          maxLength: 2000,
          maxLines: 4,
          serverError: serverErrors['description'],
        ),
      ],
    );
  }
}
