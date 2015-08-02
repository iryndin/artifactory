const ARCHIVE_MARKER = '!';
const COMPACT_FOLDERS_KEY = 'COMPACT_FOLDERS';

export class TreeBrowserDao {
    constructor(RESOURCE, ArtifactoryHttpClient, $q, $injector, ArtifactoryStorage) {
        this.$q = $q;
        this.TreeNode = $injector.get('TreeNode');
        this.RESOURCE = RESOURCE;
        this.artifactoryHttpClient = ArtifactoryHttpClient;
        this.artifactoryStorage = ArtifactoryStorage;
        this.compactFolders = ArtifactoryStorage.getItem(COMPACT_FOLDERS_KEY, /* defaultValue = */ true);
        this.roots = null;
    }

    setCompactFolders(value) {
        // TODO: save to local storage
        this.compactFolders = value;
        this.artifactoryStorage.setItem(COMPACT_FOLDERS_KEY, this.compactFolders);
    }

    getCompactFolders() {
        return this.compactFolders;
    }

    getRoots(force = false) {
        if (force || !this.roots) {
            this.roots = this._loadChildren({type: 'root'});
        }
        return this.roots;
    }

    findRepo(repoKey) {
        return this.getRoots().then((roots) => {
            return _.findWhere(roots, {repoKey: repoKey});
        });
    }

    invalidateRoots() {
        this.roots = null;
    }

    findNodeByFullPath(path, includeArchives = true) {
        if (typeof(path) === 'string') path = path.split('/');
        path = path.map((pathElement) => _.trimRight(pathElement, ARCHIVE_MARKER));
        if (!path.length) return this.$q.when(null);
        let pathElement = path.shift();
        // Find child:
        return this.getRoots().then((roots) => {
            return _.findWhere(roots, {text: pathElement});
        })
        .then((root) => {
        // Recursively look for rest of fullpath:
            if (root) return root.findNodeByPath(path, 0, includeArchives);
            else return this;
        }).catch(() => null);
    }

    _loadChildren(node, parent) {
        return this.artifactoryHttpClient.post(this.RESOURCE.TREE_BROWSER + '?compacted=' + this.compactFolders, node)
                .then(result => this._transformData(result.data, parent));

    }

    // Wrap with TreeNode
    _transformData(data, parent) {
        return data.map((node) => {
            node = new this.TreeNode(node);
            node.parent = parent;
            if (_.isArray(node.children)) {
                node.children = this.$q.when(this._transformData(node.children, node));
            }
            return node;
        });
    }
}