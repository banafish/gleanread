import { BaseEdge, type EdgeProps } from "reactflow";
import { buildKnowledgeTreeBezierPath, type GraphEdgePosition } from "@/features/knowledge-tree/graphPaths";

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
  const edgePath = buildKnowledgeTreeBezierPath({
    sourceX,
    sourceY,
    sourcePosition: sourcePosition as GraphEdgePosition,
    targetX,
    targetY,
    targetPosition: targetPosition as GraphEdgePosition,
  });

  return <BaseEdge id={id} path={edgePath} style={style} interactionWidth={interactionWidth} />;
}
