import { BaseEdge, getBezierPath, type EdgeProps } from "reactflow";

export function KnowledgeTreeEdge({
  id,
  sourceX,
  sourceY,
  sourcePosition,
  targetX,
  targetY,
  targetPosition,
  style,
  interactionWidth,
}: EdgeProps) {
  const [edgePath] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
    curvature: 0.42,
  });

  return <BaseEdge id={id} path={edgePath} style={style} interactionWidth={interactionWidth} />;
}
