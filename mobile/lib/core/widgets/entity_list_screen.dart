import 'dart:async';

import 'package:flutter/material.dart';

import '../network/api_exception.dart';

/// A filter chip on [EntityListScreen]; a null [value] means "everything".
class ListFilter {
  const ListFilter(this.label, this.value);

  final String label;
  final String? value;
}

/// Shared list screen for a user's records: loading / error / empty states,
/// pull-to-refresh, optional search and filter chips, swipe-to-delete and an add/edit form.
class EntityListScreen<T> extends StatefulWidget {
  const EntityListScreen({
    super.key,
    required this.title,
    required this.addLabel,
    required this.emptyIcon,
    required this.emptyText,
    required this.load,
    required this.delete,
    required this.idOf,
    required this.deletePrompt,
    required this.itemBuilder,
    required this.formBuilder,
    this.headerBuilder,
    this.searchHint,
    this.filters,
  });

  final String title;
  final String addLabel;
  final IconData emptyIcon;
  final String emptyText;

  /// Loads records; [query] is null when search is off or empty, [filter] is
  /// the selected [ListFilter.value].
  final Future<List<T>> Function(String? query, String? filter) load;
  final Future<void> Function(T item) delete;
  final Object Function(T item) idOf;
  final String Function(T item) deletePrompt;
  final Widget Function(BuildContext context, T item, VoidCallback onTap) itemBuilder;

  /// Builds the add (item == null) or edit form. The form pops `true` after saving.
  final Widget Function(T? item) formBuilder;
  final Widget Function(List<T> items)? headerBuilder;

  /// Enables the search field when non-null.
  final String? searchHint;

  /// Shows a row of choice chips when non-null; the first one starts selected.
  final List<ListFilter>? filters;

  @override
  State<EntityListScreen<T>> createState() => _EntityListScreenState<T>();
}

class _EntityListScreenState<T> extends State<EntityListScreen<T>> {
  final _searchController = TextEditingController();
  Timer? _debounce;
  List<T>? _items;
  String? _error;
  late String? _filter = widget.filters?.first.value;
  int _requestId = 0;

  String? get _query {
    final text = _searchController.text.trim();
    return text.isEmpty ? null : text;
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    // Ignore responses that arrive after a newer search was started.
    final requestId = ++_requestId;
    try {
      final items = await widget.load(_query, _filter);
      if (!mounted || requestId != _requestId) return;
      setState(() {
        _items = items;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted || requestId != _requestId) return;
      setState(() => _error = e.message);
    }
  }

  void _onSearchChanged(String _) {
    setState(() {}); // toggles the clear button
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), _load);
  }

  Future<void> _openForm([T? item]) async {
    final saved = await Navigator.of(context).push<bool>(MaterialPageRoute(builder: (_) => widget.formBuilder(item)));
    if (saved == true) await _load();
  }

  Future<bool> _confirmDelete(T item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Silinsin mi?'),
        content: Text(widget.deletePrompt(item)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Vazgeç')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Sil')),
        ],
      ),
    );
    if (confirmed != true) return false;

    try {
      await widget.delete(item);
      final id = widget.idOf(item);
      if (mounted) setState(() => _items = _items?.where((i) => widget.idOf(i) != id).toList());
      return true;
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      return false;
    }
  }

  void _onFilterSelected(String? value) {
    if (value == _filter) return;
    setState(() {
      _filter = value;
      _items = null; // show the spinner instead of stale results
      _error = null;
    });
    _load();
  }

  PreferredSizeWidget? _buildAppBarBottom() {
    final filters = widget.filters;
    if (widget.searchHint == null && filters == null) return null;
    return PreferredSize(
      preferredSize: Size.fromHeight((widget.searchHint == null ? 0 : 64) + (filters == null ? 0 : 52)),
      child: Column(
        children: [
          if (widget.searchHint != null) _buildSearchBar(),
          if (filters != null)
            SizedBox(
              height: 52,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                children: [
                  for (final filter in filters)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(filter.label),
                        selected: filter.value == _filter,
                        onSelected: (_) => _onFilterSelected(filter.value),
                      ),
                    ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: SearchBar(
        controller: _searchController,
        hintText: widget.searchHint,
        leading: const Icon(Icons.search),
        elevation: const WidgetStatePropertyAll(0),
        onChanged: _onSearchChanged,
        trailing: [
          if (_searchController.text.isNotEmpty)
            IconButton(
              tooltip: 'Temizle',
              icon: const Icon(Icons.close),
              onPressed: () {
                _debounce?.cancel();
                setState(_searchController.clear);
                _load();
              },
            ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.title), bottom: _buildAppBarBottom()),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openForm(),
        icon: const Icon(Icons.add),
        label: Text(widget.addLabel),
      ),
      body: RefreshIndicator(onRefresh: _load, child: _buildBody()),
    );
  }

  Widget _buildBody() {
    final items = _items;
    if (items == null && _error == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (items == null) {
      return _Message(
        icon: Icons.cloud_off,
        text: _error!,
        action: FilledButton.tonal(onPressed: _load, child: const Text('Tekrar dene')),
      );
    }
    if (items.isEmpty) {
      if (_query != null) return const _Message(icon: Icons.search_off, text: 'Aramanla eşleşen kayıt yok.');
      if (_filter != null) return const _Message(icon: Icons.filter_alt_off, text: 'Bu filtrede kayıt yok.');
      return _Message(icon: widget.emptyIcon, text: widget.emptyText);
    }

    final header = widget.headerBuilder;
    final offset = header == null ? 0 : 1;
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.only(bottom: 96),
      itemCount: items.length + offset,
      itemBuilder: (context, index) {
        if (header != null && index == 0) return header(items);
        final item = items[index - offset];
        return Dismissible(
          key: ValueKey(widget.idOf(item)),
          direction: DismissDirection.endToStart,
          confirmDismiss: (_) => _confirmDelete(item),
          background: Container(
            color: Theme.of(context).colorScheme.errorContainer,
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Icon(Icons.delete_outline, color: Theme.of(context).colorScheme.onErrorContainer),
          ),
          child: widget.itemBuilder(context, item, () => _openForm(item)),
        );
      },
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({required this.icon, required this.text, this.action});

  final IconData icon;
  final String text;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    // Scrollable so pull-to-refresh also works on empty and error states.
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(32, 120, 32, 32),
      children: [
        Icon(icon, size: 56, color: Theme.of(context).colorScheme.outline),
        const SizedBox(height: 16),
        Text(text, textAlign: TextAlign.center),
        if (action != null) ...[const SizedBox(height: 16), Center(child: action)],
      ],
    );
  }
}
