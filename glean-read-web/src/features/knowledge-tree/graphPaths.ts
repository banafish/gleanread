export type GraphEdgePosition = "left" | "top" | "right" | "bottom";

export const KNOWLEDGE_TREE_EDGE_CURVATURE = 0.42;

interface BezierPathOptions {
  sourceX: number;
  sourceY: number;
  sourcePosition?: GraphEdgePosition;
  targetX: number;
  targetY: number;
  targetPosition?: GraphEdgePosition;
  curvature?: number;
}

function calculateControlOffset(distance: number, curvature: number): number {
  if (distance >= 0) {
    return 0.5 * distance;
  }
  return curvature * 25 * Math.sqrt(-distance);
}

function getControlWithCurvature({
  position,
  x1,
  y1,
  x2,
  y2,
  curvature,
}: {
  position: GraphEdgePosition;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  curvature: number;
}): [number, number] {
  switch (position) {
    case "left":
      return [x1 - calculateControlOffset(x1 - x2, curvature), y1];
    case "right":
      return [x1 + calculateControlOffset(x2 - x1, curvature), y1];
    case "top":
      return [x1, y1 - calculateControlOffset(y1 - y2, curvature)];
    case "bottom":
      return [x1, y1 + calculateControlOffset(y2 - y1, curvature)];
  }
}

export function buildKnowledgeTreeBezierPath({
  sourceX,
  sourceY,
  sourcePosition = "bottom",
  targetX,
  targetY,
  targetPosition = "top",
  curvature = KNOWLEDGE_TREE_EDGE_CURVATURE,
}: BezierPathOptions): string {
  const [sourceControlX, sourceControlY] = getControlWithCurvature({
    position: sourcePosition,
    x1: sourceX,
    y1: sourceY,
    x2: targetX,
    y2: targetY,
    curvature,
  });
  const [targetControlX, targetControlY] = getControlWithCurvature({
    position: targetPosition,
    x1: targetX,
    y1: targetY,
    x2: sourceX,
    y2: sourceY,
    curvature,
  });

  return `M${sourceX},${sourceY} C${sourceControlX},${sourceControlY} ${targetControlX},${targetControlY} ${targetX},${targetY}`;
}
