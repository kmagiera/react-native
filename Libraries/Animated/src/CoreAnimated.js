
const ATTACHED_PROPS_SET = new Set();
const UPDATED_NODES = [];

let loopID = 1;
let propUpdatesEnqueued = null;

function findAndUpdateNodes(node) {
    if (typeof node.update === 'function') {
        node.update()
    } else {
        node.__getChildren().forEach(findAndUpdateNodes);
    }
}

function runPropUpdates() {
    const visitedNodes = new Set();
    for (let i = 0; i < UPDATED_NODES.length; i++) {
        const node = UPDATED_NODES[i];
        if (!visitedNodes.has(node)) {
            visitedNodes.add(node);
            findAndUpdateNodes(node);
        }
    }
    UPDATED_NODES.length = 0; // clear array
    propUpdatesEnqueued = null;
    loopID += 1;
}

function onNodeUpdated(node) {
    UPDATED_NODES.push(node);
    if (!propUpdatesEnqueued) {
        propUpdatesEnqueued = setImmediate(runPropUpdates);
    }
}

function evaluate(node) {
    if (node.__lastLoopID < loopID) {
        node.__lastLoopID = loopID;
        return node.__memoizedValue = node.__onEvaluate();
    }
    return node.__memoizedValue;
}

module.exports = {
    onNodeUpdated,
    evaluate,
}
