import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/widgets/entity_list_screen.dart';
import '../../../core/widgets/form_fields.dart';
import '../data/media_repository.dart';
import '../models/media.dart';
import 'media_form_screen.dart';

class MediaScreen extends StatelessWidget {
  const MediaScreen({super.key});

  static IconData iconFor(String type) => switch (type.toLowerCase()) {
        'film' || 'movie' => Icons.movie_outlined,
        'dizi' || 'series' => Icons.live_tv_outlined,
        'belgesel' => Icons.public,
        'podcast' => Icons.podcasts,
        'video' => Icons.smart_display_outlined,
        _ => Icons.play_circle_outline,
      };

  @override
  Widget build(BuildContext context) {
    final repository = MediaRepository(AppScope.read(context).api);
    return EntityListScreen<Media>(
      title: 'Medya',
      addLabel: 'Medya ekle',
      emptyIcon: Icons.movie,
      emptyText: 'Henüz medya eklemedin.\nİzlediğin film, dizi ya da dinlediğin podcastleri ekle.',
      searchHint: 'Başlık, tür veya açıklamada ara',
      filters: [
        const ListFilter('Tümü', null),
        for (final status in MediaStatus.values) ListFilter(status.label, status.apiValue),
      ],
      load: (query, filter, page) => repository.fetchMedia(
        query: query,
        status: filter == null ? null : MediaStatus.fromApi(filter),
        page: page,
      ),
      delete: (media) => repository.deleteMedia(media.id),
      idOf: (media) => media.id,
      deletePrompt: (media) => '"${media.title}" silinsin mi?',
      itemBuilder: (context, media, onTap) => ListTile(
        onTap: onTap,
        leading: CircleAvatar(child: Icon(iconFor(media.type))),
        title: Text(media.title),
        subtitle: Text(
          [
            [media.type, media.status.label, if (media.rating != null) formatStars(media.rating!)].join(' · '),
            if (media.description != null) media.description!,
          ].join('\n'),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        isThreeLine: media.description != null,
      ),
      formBuilder: (media) => MediaFormScreen(repository: repository, media: media),
    );
  }
}
