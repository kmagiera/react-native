
const ATTACHED_PROPS_SET = new Set();
const UPDATED_NODES = new Set();
let enqueuedAnimations = [];

let loopTs = +new Date();
let propUpdatesEnqueued = null;

function findUpdatedStyleNodes() {
    const animatedStyles = new Set();
    function findAnimatedStyles(node) {
        if (typeof node.update === 'function') {
            animatedStyles.add(node);
        } else {
            node.__getChildren().forEach(findAnimatedStyles);
        }
    }
    UPDATED_NODES.forEach(findAnimatedStyles);
    UPDATED_NODES.clear();
    return animatedStyles;
}

function runPropUpdates() {
    propUpdatesEnqueued = null;
    findUpdatedStyleNodes().forEach(style => style.update());
}


function runAnimations() {
    loopTs = +new Date()
    const animations = enqueuedAnimations;
    enqueuedAnimations = [];
    animations.forEach(anim => {
        anim();
    })
    if (!propUpdatesEnqueued) {
        setImmediate(runPropUpdates);
    }
}

function wantNextFrame(animation) {
    enqueuedAnimations.push(animation);
    if (enqueuedAnimations.length === 1) {
        requestAnimationFrame(runAnimations);
    }
}

function onNodeUpdated(node) {
    UPDATED_NODES.add(node);
}

function evaluate(node) {
    if (node.__lastLoopTs < loopTs) {
        node.__lastLoopTs = loopTs;
        return node.__memoizedValue = node.__onEvaluate();
    }
    return node.__memoizedValue;
}

module.exports = {
    onNodeUpdated,
    evaluate,
    wantNextFrame,
}
