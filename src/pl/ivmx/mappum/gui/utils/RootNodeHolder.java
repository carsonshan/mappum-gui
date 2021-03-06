package pl.ivmx.mappum.gui.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jrubyparser.ast.NodeType;
import org.jrubyparser.BlockStaticScope;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.StaticScope;
import org.jrubyparser.ast.ArrayNode;
import org.jrubyparser.ast.BlockNode;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ConstNode;
import org.jrubyparser.ast.DAsgnNode;
import org.jrubyparser.ast.DVarNode;
import org.jrubyparser.ast.FCallNode;
import org.jrubyparser.ast.FixnumNode;
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.ListNode;
import org.jrubyparser.ast.MultipleAsgnNode;
import org.jrubyparser.ast.NewlineNode;
import org.jrubyparser.ast.NilImplicitNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.RootNode;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.VCallNode;
import org.jrubyparser.ast.XStrNode;

import pl.ivmx.mappum.gui.model.Connection;
import pl.ivmx.mappum.gui.model.Shape;

public class RootNodeHolder {
	public static final int LEFT_DASGN_CHANGE = 0;
	public static final int RIGHT_DASGN_CHANGE = 1;
	public static final int BOTH_DASGN_CHANGE = 2;
	public static final int IDENT_LENGTH = 1;
	private static final RootNodeHolder INSTANCE = new RootNodeHolder();
	private static final String MAPPUM_STR = "mappum";
	private static final String REQUIRE_STR = "require";
	private static final String CATALOGUE_STR = "catalogue_add";
	public static final int LEFT_ELEM_ARRAY_CHANGE = 1;
	public static final int RIGHT_ELEM_ARRAY_CHANGE = 2;

	private Node rootNode;
	private List<String> usedIdent = new ArrayList<String>();
	private int usedCharIndex = 0;
	private char[] charArray = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z' };
	private String lastNotSelfLeftVariable = "";
	private String lastNotSelfRightVariable = "";

	// private Logger logger = Logger.getLogger(RootNodeHolder.class);

	private RootNodeHolder() {
	}

	public static final RootNodeHolder getInstance() {
		return INSTANCE;
	}

	public Node getRootNode() {
		return rootNode;
	}

	public void setRootNode(Node rootNode) {
		usedCharIndex = 0;
		this.rootNode = rootNode;
	}

	public void addMappingNewlineNode(NewlineNode node) {
		findRootBlockNode(rootNode).add(node);
	}

	public BlockNode findRootBlockNode(Node node) {
		boolean iterate = true;
		BlockNode blockNode = null;
		for (Node child : node.childNodes()) {
			if (child instanceof FCallNode) {
				if ("map".equals(((FCallNode) child).getName())) {
					iterate = false;
					if (((FCallNode) child).getIterNode() != null) {
						blockNode = (BlockNode) ((FCallNode) child)
								.getIterNode().childNodes().get(1);
					}

				}
			}
			if (iterate == true)
				blockNode = findRootBlockNode(child);
		}
		return blockNode;
	}

	private NewlineNode findRootMappingNode(Node node) {
		for (Node child : node.childNodes()) {
			if (child instanceof FCallNode) {
				if ("map".equals(((FCallNode) child).getName())) {
					return (NewlineNode) node;
				}
			}
			if (findRootBlockNode(child) != null)
				return findRootMappingNode(child);
		}
		return null;
	}

	private BlockNode getBlockNode(Node node) {
		if (node.childNodes().get(0) instanceof FCallNode) {
			if (node.childNodes().get(0).childNodes().size() > 1) {
				if (node.childNodes().get(0).childNodes().get(1) instanceof IterNode) {
					return (BlockNode) ((IterNode) node.childNodes().get(0)
							.childNodes().get(1)).getBodyNode();
				}
			}
		}
		return null;
	}

	private String[] findDVars(Node node) {
		String[] dvarsArray = new String[2];
		for (Node child : node.childNodes()) {
			if (child instanceof FCallNode) {
				if (((FCallNode) child).getName().equals("map")) {
					dvarsArray[0] = ((DAsgnNode) ((FCallNode) child)
							.getIterNode().childNodes().get(0).childNodes()
							.get(0).childNodes().get(0)).getName();
					usedIdent.add(dvarsArray[0]);
					dvarsArray[1] = ((DAsgnNode) ((FCallNode) child)
							.getIterNode().childNodes().get(0).childNodes()
							.get(0).childNodes().get(1)).getName();
					usedIdent.add(dvarsArray[1]);
				}
			}
		}
		return dvarsArray;
	}

	public boolean changeMappingAtributes(Connection connection,
			String newSide, String newComment) {
		try{
		NewlineNode newlineNode = connection.getRubyCodeNode();
		CallNode callnode;
		if (newlineNode.getNextNode() instanceof CallNode) {
			callnode = (CallNode) newlineNode.getNextNode();
		} else {
			callnode = (CallNode) ((FCallNode) newlineNode.getNextNode())
					.getArgsNode().childNodes().get(0);
		}

		if (newSide != null) {
			callnode.setName(newSide);
			return true;
		} else if (newComment != null) {
			NewlineNode comment = generateComment(newComment);
			int n = 0;
			Node parent = getParentNode(newlineNode, rootNode);
			Node line = newlineNode;
			while(! (parent instanceof BlockNode)) {
				line = parent;
				parent = getParentNode(parent, rootNode);	
			}
			for (Node tmpNode : parent.childNodes()) {
				if (tmpNode.equals(line)) {
					break;
				}
				n++;
			}
			if (connection.getComment() == null
					|| connection.getComment().equals("")) {
				parent.childNodes().add(n, comment);
				return true;
			} else {
				if (comment != null) {
					parent.childNodes().add(n, comment);
				}
				int offset = 1;
				do {
					if (offset > n) {
						break;
					}
					final Node nodeToDelete = parent.childNodes().get(
							n - offset);
					if (nodeToDelete.getNodeType().equals(NodeType.NEWLINENODE)) {
						if (((NewlineNode) nodeToDelete).getNextNode()
								.getNodeType().equals(NodeType.XSTRNODE)) {
							parent.childNodes().remove(n - offset++);
							continue;
						}
					}
					break;
				} while (true);

				return true;
			}
		}
		return false;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Node getParentNode(final Node nodeToFind, final Node nodeToSearchIn) {
		if (nodeToSearchIn == null || nodeToFind == null) {
			return null;
		}
		Node node = null;
		for (Node child : nodeToSearchIn.childNodes()) {
			if (nodeToFind.equals(child)) {
				return nodeToSearchIn;
			} else {
				node = getParentNode(nodeToFind, child);
				if (node != null)
					return node;
			}
		}
		return node;
	}

	private String appendToVariableName(final String name, final String newName) {
		if (name == null || "".equals(name)) {
			return newName;
		}
		return name + "." + newName;
	}

	public NewlineNode findMappingNode(final Connection connection,
			final Node node) {
		return findMappingNode(connection, node, "", "");
	}

	public NewlineNode findMappingNode(Connection connection, Node node,
			String inLeftVariable, String inRightVariable) {
		NewlineNode newlineNode = null;
		if (node instanceof BlockNode) {
			for (Node newline : node.childNodes()) {
				for (Node child : newline.childNodes()) {
					String leftVariable = inLeftVariable;
					String rightVariable = inRightVariable;
					if (child instanceof FCallNode) {
						if (((FCallNode) child).getName().equals("map")) {
							if ((((FCallNode) child).getArgsNode()) instanceof ArrayNode) {
								if ((((FCallNode) child).getArgsNode())
										.childNodes().get(0) instanceof CallNode) {
									CallNode callnode = (CallNode) (((FCallNode) child)
											.getArgsNode()).childNodes().get(0);
									// if (Connection
									// .translateSideFromIntToString(
									// connection.getMappingSide())
									// .equals(callnode.getName())) {
									if (connection.getConnectionType() == Connection.Type.VAR_TO_VAR_CONN) {
										CallNode leftNode = ModelGenerator
												.findLastCallNodeInTree((callnode
														.getReceiverNode()));
										CallNode rightNode = ModelGenerator
												.findLastCallNodeInTree(callnode
														.getArgsNode()
														.childNodes().get(0));
										if (leftNode != null
												&& rightNode != null) {

											final String curLeftVariable = leftNode
													.getName();
											if (!curLeftVariable.equals("self")) {
												lastNotSelfLeftVariable = curLeftVariable;
												leftVariable = appendToVariableName(
														leftVariable,
														curLeftVariable);
											}

											final String curRightVariable = rightNode
													.getName();

											if (!curRightVariable
													.equals("self")) {
												lastNotSelfRightVariable = curRightVariable;
												rightVariable = appendToVariableName(
														rightVariable,
														curRightVariable);
											}

											if (Connection
													.translateSideFromIntToString(
															connection
																	.getMappingSide())
													.equals(callnode.getName())) {
												if (leftVariable
														.equals(connection
																.getFullSourceName())
														&& rightVariable
																.equals(connection
																		.getFullTargetName())) {
													if (connection
															.getArrayNumber() > -1) {
														if (checkFirstNodeArrayElementNumber(
																(NewlineNode) newline,
																connection
																		.getArrayNumber())) {
															return (NewlineNode) newline;
														}
													} else {
														return (NewlineNode) newline;
													}

												}
											}
										}
									} else if (connection.getConnectionType() == Connection.Type.CONST_TO_VAR_CONN) {
										if (callnode.getReceiverNode() instanceof StrNode) {
											StrNode leftNode = ((StrNode) callnode
													.getReceiverNode());
											CallNode rightNode = ModelGenerator
													.findLastCallNodeInTree(callnode
															.getArgsNode()
															.childNodes()
															.get(0));

											if (leftNode != null
													&& rightNode != null) {

												final String curLeftVariable = leftNode
														.getValue();
												final String curRightVariable = rightNode
														.getName();

												rightVariable = appendToVariableName(
														rightVariable,
														curLeftVariable);

												if (Connection
														.translateSideFromIntToString(
																connection
																		.getMappingSide())
														.equals(
																callnode
																		.getName())) {
													if (curLeftVariable
															.equals(connection
																	.getConstantName())
															&& curRightVariable
																	.equals(connection
																			.getFullTargetName())) {
														if (connection
																.getArrayNumber() > -1) {
															if (checkFirstNodeArrayElementNumber(
																	(NewlineNode) newline,
																	connection
																			.getArrayNumber())) {
																return (NewlineNode) newline;
															}
														} else {
															return (NewlineNode) newline;
														}
													}
												}
											}
										} else if (callnode.getArgsNode()
												.childNodes().get(0) instanceof StrNode) {
											CallNode leftNode = ModelGenerator
													.findLastCallNodeInTree((callnode
															.getReceiverNode()));
											StrNode rightNode = (StrNode) callnode
													.getArgsNode().childNodes()
													.get(0);

											if (leftNode != null
													&& rightNode != null) {

												final String curLeftVariable = leftNode
														.getName();
												final String curRightVariable = rightNode
														.getValue();

												leftVariable = appendToVariableName(
														leftVariable,
														curLeftVariable);

												if (Connection
														.translateSideFromIntToString(
																connection
																		.getMappingSide())
														.equals(
																callnode
																		.getName())) {
													if (leftVariable
															.equals(connection
																	.getFullSourceName())
															&& curRightVariable
																	.equals(connection
																			.getConstantName())) {
														if (connection
																.getArrayNumber() > -1) {
															if (checkFirstNodeArrayElementNumber(
																	(NewlineNode) newline,
																	connection
																			.getArrayNumber())) {
																return (NewlineNode) newline;
															}
														} else {
															return (NewlineNode) newline;
														}
													}
												}
											}
										}
									}
									//
									else if (connection.getConnectionType() == Connection.Type.FUN_TO_VAR_CONN) {
										if (callnode.getReceiverNode() instanceof VCallNode) {
											VCallNode leftNode = ((VCallNode) callnode
													.getReceiverNode());
											CallNode rightNode = ModelGenerator
													.findLastCallNodeInTree(callnode
															.getArgsNode()
															.childNodes()
															.get(0));

											if (leftNode != null
													&& rightNode != null) {

												final String curLeftVariable = leftNode
														.getName();
												final String curRightVariable = rightNode
														.getName();
												rightVariable = appendToVariableName(
														rightVariable,
														curRightVariable);

												if (Connection
														.translateSideFromIntToString(
																connection
																		.getMappingSide())
														.equals(
																callnode
																		.getName())) {
													if (curLeftVariable
															.equals("func")
															&& rightVariable
																	.equals(connection
																			.getFullTargetName())) {
														if (connection
																.getArrayNumber() > -1) {
															if (checkFirstNodeArrayElementNumber(
																	(NewlineNode) newline,
																	connection
																			.getArrayNumber())) {
																return (NewlineNode) newline;
															}
														} else {
															return (NewlineNode) newline;
														}
													}
												}
											}
										} else if (callnode.getArgsNode()
												.childNodes().get(0) instanceof VCallNode) {
											CallNode leftNode = ModelGenerator
													.findLastCallNodeInTree((callnode
															.getReceiverNode()));
											VCallNode rightNode = (VCallNode) callnode
													.getArgsNode().childNodes()
													.get(0);

											if (leftNode != null
													&& rightNode != null) {

												final String curLeftVariable = leftNode
														.getName();
												final String curRightVariable = rightNode
														.getName();
												leftVariable = appendToVariableName(
														leftVariable,
														curLeftVariable);

												if (Connection
														.translateSideFromIntToString(
																connection
																		.getMappingSide())
														.equals(
																callnode
																		.getName())) {
													if (leftVariable
															.equals(connection
																	.getFullSourceName())
															&& curRightVariable
																	.equals("func")) {
														if (connection
																.getArrayNumber() > -1) {
															if (checkFirstNodeArrayElementNumber(
																	(NewlineNode) newline,
																	connection
																			.getArrayNumber())) {
																return (NewlineNode) newline;
															}
														} else {
															return (NewlineNode) newline;
														}
													}
												}
											}
										}
									}
								}
							}
							if ((((FCallNode) child).getIterNode()) != null
									&& (((FCallNode) child).getIterNode()) instanceof IterNode) {
								IterNode iterNode = (IterNode) ((FCallNode) child)
										.getIterNode();
								if (iterNode.getBodyNode() != null
										&& iterNode.getBodyNode() instanceof BlockNode) {
									BlockNode blockNode = (BlockNode) iterNode
											.getBodyNode();
									for (Node newlineChild : blockNode
											.childNodes()) {
										if (newlineChild instanceof NewlineNode) {
											if (((NewlineNode) newlineChild)
													.getNextNode() instanceof FCallNode) {

												CallNode mappingCallNode = getMappingCallNode((NewlineNode) newlineChild);
												String leftChildVariable = ModelGenerator
														.findLastCallNodeInTree(
																(mappingCallNode
																		.getReceiverNode()))
														.getName();
												String rightChildVariable = ModelGenerator
														.findLastCallNodeInTree(
																mappingCallNode
																		.getArgsNode()
																		.childNodes()
																		.get(0))
														.getName();
												if (leftChildVariable
														.equals("self")
														|| rightChildVariable
																.equals("self")) {
													if ((leftChildVariable
															.equals(connection
																	.getSource()
																	// .getShapeNode()
																	.getName()) && lastNotSelfRightVariable
															.equals(connection
																	.getTarget()
																	// .getShapeNode()
																	.getName()))
															|| (lastNotSelfLeftVariable
																	.equals(connection
																			.getSource()
																			// .getShapeNode()
																			.getName()) && rightChildVariable
																	.equals(connection
																			.getTarget()
																			// .getShapeNode()
																			.getName()))) {
														if (connection
																.getArrayNumber() > -1) {
															if (checkFirstNodeArrayElementNumber(
																	(NewlineNode) newlineChild,
																	connection
																			.getArrayNumber())) {
																return (NewlineNode) newlineChild;
															}
														} else {
															return (NewlineNode) newlineChild;
														}
													}
												}
											}
										}
									}

								}
							}
						}
					} else if (child instanceof CallNode
							&& connection.getConnectionType() == Connection.Type.CONST_TO_VAR_CONN) {
						if (child.childNodes().size() > 0) {
							CallNode callnode = (CallNode) child;
							if (callnode.getReceiverNode() instanceof StrNode) {
								StrNode leftNode = ((StrNode) callnode
										.getReceiverNode());
								CallNode rightNode = ModelGenerator
										.findLastCallNodeInTree(callnode
												.getArgsNode().childNodes()
												.get(0));

								if (leftNode != null && rightNode != null) {

									final String curLeftVariable = leftNode
											.getValue();
									final String curRightVariable = rightNode
											.getName();
									rightVariable = appendToVariableName(
											rightVariable, curRightVariable);

									if (Connection
											.translateSideFromIntToString(
													connection.getMappingSide())
											.equals(callnode.getName())) {
										if (curLeftVariable.equals(connection
												.getConstantName())
												&& rightVariable
														.equals(connection
																.getFullTargetName())) {
											if (connection.getArrayNumber() > -1) {
												if (checkFirstNodeArrayElementNumber(
														(NewlineNode) newline,
														connection
																.getArrayNumber())) {
													return (NewlineNode) newline;
												}
											} else {
												return (NewlineNode) newline;
											}
										}
									}
								}
							} else if ((callnode.getArgsNode().childNodes()
									.size() > 0)
									&& (callnode.getArgsNode().childNodes()
											.get(0) instanceof StrNode)) {
								CallNode leftNode = ModelGenerator
										.findLastCallNodeInTree((callnode
												.getReceiverNode()));
								StrNode rightNode = (StrNode) callnode
										.getArgsNode().childNodes().get(0);

								if (leftNode != null && rightNode != null) {

									final String curLeftVariable = leftNode
											.getName();
									final String curRightVariable = rightNode
											.getValue();

									leftVariable = appendToVariableName(
											leftVariable, curLeftVariable);

									if (Connection
											.translateSideFromIntToString(
													connection.getMappingSide())
											.equals(callnode.getName())) {
										if (leftVariable.equals(connection
												.getFullSourceName())
												&& curRightVariable
														.equals(connection
																.getConstantName())) {
											if (connection.getArrayNumber() > -1) {
												if (checkFirstNodeArrayElementNumber(
														(NewlineNode) newline,
														connection
																.getArrayNumber())) {
													return (NewlineNode) newline;
												}
											} else {
												return (NewlineNode) newline;
											}
										}
									}
								}
							}
						}
					}
					if (getBlockNode((NewlineNode) newline) != null) {
						newlineNode = findMappingNode(connection,
								(getBlockNode((NewlineNode) newline)),
								leftVariable, rightVariable);
						if (newlineNode != null) {
							return newlineNode;
						}
					}

				}
			}
		}
		return newlineNode;
	}

	private boolean checkIfMappingIsVarToArray(NewlineNode node, int elementNum) {
		if (node != null) {
			Node fCallNode = node.getNextNode();
			if (fCallNode != null && fCallNode instanceof FCallNode
					&& ((FCallNode) fCallNode).getName().equals("map")) {
				Node callNode = fCallNode.childNodes().get(0).childNodes().get(
						0);
				if (callNode != null && callNode instanceof CallNode) {
					if (findFixNumNode(callNode.childNodes().get(0), null) != findFixNumNode(
							callNode.childNodes().get(1), null)) {
						if (findFixNumNode(callNode.childNodes().get(0),
								elementNum)) {
							return true;
						} else if (findFixNumNode(callNode.childNodes().get(1),
								elementNum)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean checkFirstNodeArrayElementNumber(NewlineNode node,
			int elementNum) {
		if (node != null) {
			Node fCallNode = node.getNextNode();
			if (fCallNode != null && fCallNode instanceof FCallNode
					&& ((FCallNode) fCallNode).getName().equals("map")) {
				Node callNode = fCallNode.childNodes().get(0).childNodes().get(
						0);
				if (callNode != null && callNode instanceof CallNode) {
					if (findFixNumNode(callNode.childNodes().get(0), null) != findFixNumNode(
							callNode.childNodes().get(1), null)) {
						if (findFixNumNode(callNode.childNodes().get(0),
								elementNum)) {
							return true;
						} else if (findFixNumNode(callNode.childNodes().get(1),
								elementNum)) {
							return true;
						}
					}
				}
			}
			if (checkFirstNodeArrayElementNumber(getParentMappingNode(node),
					elementNum)) {
				return true;
			}
		}
		return false;
	}

	private boolean findFixNumNode(Node node, Integer fixNum) {
		if (node instanceof FixnumNode) {
			if (fixNum != null) {
				if (((FixnumNode) node).getValue() == fixNum) {
					return true;
				} else {
					return false;
				}
			} else {
				return true;
			}
		} else {
			for (Node child : node.childNodes()) {
				if (findFixNumNode(child, fixNum) == true) {
					return true;
				}
			}
		}
		return false;
	}

	private CallNode[] addArrayElementDeclaration(CallNode leftSide,
			CallNode rightSide, Integer arrayElementSide, Integer arrayNumber) {
		if (arrayElementSide != null) {
			CallNode arrayNode = null;
			if (arrayElementSide.equals(LEFT_ELEM_ARRAY_CHANGE)) {
				if (leftSide.getName().equals("[]")) {
					arrayNode = leftSide;
				}
			} else if (arrayElementSide.equals(RIGHT_ELEM_ARRAY_CHANGE)) {
				if (rightSide.getName().equals("[]")) {
					arrayNode = rightSide;
				}
			}
			if (arrayNode != null) {
				Node parentNode = getParentNode(arrayNode, rootNode);

				FixnumNode fixnumNode = new FixnumNode(new SourcePosition(),
						arrayNumber.longValue());
				ArrayNode node = new ArrayNode(new SourcePosition(), fixnumNode);
				CallNode newArrayNode = new CallNode(new SourcePosition(),
						arrayNode.getReceiverNode(), arrayNode.getName(), node,
						arrayNode.getIterNode());
				if (parentNode instanceof CallNode) {
					CallNode callNode = (CallNode) parentNode;
					if (callNode.getArgsNode().equals(arrayNode)) {
						callNode.setArgsNode(newArrayNode);
					} else if (callNode.getIterNode().equals(arrayNode)) {
						callNode.setIterNode(newArrayNode);
					} else if (callNode.getReceiverNode().equals(arrayNode)) {
						callNode.setReceiverNode(arrayNode);
					}
				} else if (parentNode instanceof ArrayNode) {
					ArrayNode aNode = (ArrayNode) parentNode;
					aNode.childNodes().set(0, arrayNode);
				}
				if (arrayElementSide.equals(RIGHT_ELEM_ARRAY_CHANGE)
						&& rightSide.getName().equals("[]")) {
					rightSide = newArrayNode;
				} else if (arrayElementSide.equals(LEFT_ELEM_ARRAY_CHANGE)
						&& leftSide.getName().equals("[]")) {
					leftSide = newArrayNode;
				}

			}
		}
		return new CallNode[] { leftSide, rightSide };
	}

	private Node createTypeNode(final String typeName) {
		final ConstNode constNode = new ConstNode(new SourcePosition(),
				typeName);
		return new ArrayNode(new SourcePosition(), constNode);
	}

	private void addTypeToNode(final CallNode node, final String typeName) {
		if (typeName != null) {
			node.setArgsNode(createTypeNode(typeName));
		}
	}

	private void addTypesToNodes(final CallNode node1, final String typeName1,
			final CallNode node2, final String typeName2) {
		if (typeName1 != null && typeName1.equals(typeName2)) {
			return;
		}
		addTypeToNode(node1, typeName1);
		addTypeToNode(node2, typeName2);
	}

	private NewlineNode generateSimpleMapping(final CallNode leftSideIn,
			final String leftType, final CallNode rightSideIn,
			final String rightType, String side, NewlineNode parentMapping,
			Integer arrayElementSide, Integer arrayNumber) {
		CallNode[] nodes = addArrayElementDeclaration(leftSideIn, rightSideIn,
				arrayElementSide, arrayNumber);

		final CallNode leftSide = nodes[0];
		final CallNode rightSide = nodes[1];

		addTypesToNodes(leftSide, leftType, rightSide, rightType);

		ArrayNode argsNode = new ArrayNode(new SourcePosition(), rightSide);

		CallNode callNode = new CallNode(new SourcePosition(), leftSide, side,
				argsNode);
		ArrayNode arrayNode = new ArrayNode(new SourcePosition(), callNode);
		FCallNode fcallMapNode = new FCallNode(new SourcePosition(), "map",
				arrayNode);
		NewlineNode newlineNode = new NewlineNode(new SourcePosition(),
				fcallMapNode);
		String[] s = findDVars(parentMapping);
		changeMappingDVars(newlineNode, s[0], s[1]);
		return newlineNode;
	}

	private NewlineNode getParentMappingNode(NewlineNode childMappingNode) {
		Node blockNode = getParentNode(childMappingNode, rootNode);
		if (blockNode != null && blockNode instanceof BlockNode) {
			Node iterNode = getParentNode(blockNode, rootNode);
			if (iterNode != null && iterNode instanceof IterNode) {
				Node fcallNode = getParentNode(iterNode, rootNode);
				if (fcallNode != null && fcallNode instanceof FCallNode) {
					Node newlineNode = getParentNode(fcallNode, rootNode);
					if (newlineNode != null
							&& newlineNode instanceof NewlineNode) {
						return (NewlineNode) newlineNode;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Generates comment Node
	 * 
	 * @param comment
	 * @return
	 */
	private NewlineNode generateComment(String comment) {
		if ("".equals(comment) || comment == null) {
			return null;
		}
		XStrNode commentNode = new XStrNode(new SourcePosition(), comment);
		return new NewlineNode(new SourcePosition(), commentNode);
	}

	/**
	 * Generates mapping tree node and add it in proper place in root tree node
	 * 
	 * @param leftShape
	 * @param rightShape
	 * @param side
	 * @param comment
	 */
	public NewlineNode addMapping(Shape leftShape, Shape rightShape, String side,
			String comment, Integer arrayNumber) {
		List<Integer> path;
		if (arrayNumber != null) {
			path = findMappingPath(leftShape, rightShape, arrayNumber);
		} else {
			path = findMappingPath(leftShape, rightShape);
		}
		NewlineNode node = findRootMappingNode(rootNode);
		for (int i : path) {
			node = (NewlineNode) getBlockNode(node).get(i);
		}
		List<Shape> leftShapeList = leftShape.getShapeStack();
		List<Shape> rightShapeList = rightShape.getShapeStack();
		Collections.reverse(leftShapeList);
		Collections.reverse(rightShapeList);
		int usedPathSize = 0;
		for (int i = 0; i < path.size(); i++) {
			if (leftShapeList.size() == 1 && rightShapeList.size() > 1) {
				rightShapeList.remove(0);
				if (leftShapeList.size() == 1 && rightShapeList.size() == 1) {
					leftShapeList.remove(0);
				}
				usedPathSize++;
			} else if (leftShapeList.size() > 1 && rightShapeList.size() == 1) {
				leftShapeList.remove(0);
				if (leftShapeList.size() == 1 && rightShapeList.size() == 1) {
					rightShapeList.remove(0);
				}
				usedPathSize++;
			}
		}

		for (int i = 0; i < path.size() - usedPathSize; i++) {
			leftShapeList.remove(0);
			rightShapeList.remove(0);
		}
		Pair lastArrayShape = findLastArrayShape(leftShapeList, rightShapeList);

		int n = 0;
		if (leftShapeList.size() > rightShapeList.size()) {
			n = leftShapeList.size();
		} else {
			n = rightShapeList.size();
		}
		NewlineNode tmpNode = null;
		NewlineNode outNode = null;
		
		for (int i = 0; i < n; i++) {

			if ((leftShapeList.size() > 1 && rightShapeList.size() > 1)
					|| (leftShapeList.size() == 1 && rightShapeList.size() > 1)
					|| (leftShapeList.size() > 1 && rightShapeList.size() == 1)) {

				final CallNode leftNode = leftShapeList.get(0).getShapeNode();
				final String leftType = leftShapeList.get(0).getType();
				final CallNode rightNode = rightShapeList.get(0).getShapeNode();
				final String rightType = rightShapeList.get(0).getType();

				if (lastArrayShape.getLeftShape() != null
						&& lastArrayShape.getLeftShape().equals(
								leftShapeList.get(0))) {
					tmpNode = generateComplexMapping(
							leftNode,
							leftType,
							rightNode,
							rightType,
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, LEFT_ELEM_ARRAY_CHANGE,
							arrayNumber);
				} else if (lastArrayShape.getRightShape() != null
						&& lastArrayShape.getRightShape().equals(
								rightShapeList.get(0))) {
					tmpNode = generateComplexMapping(
							leftNode,
							leftType,
							rightNode,
							rightType,
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, RIGHT_ELEM_ARRAY_CHANGE,
							arrayNumber);
				} else {
					tmpNode = generateComplexMapping(
							leftNode,
							leftType,
							rightNode,
							rightType,
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, null, null);
				}

				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				//empty line
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));
				leftShapeList.remove(0);
				rightShapeList.remove(0);
			}

			else if (leftShapeList.size() > 1 && rightShapeList.size() == 0) {
				ListNode listNode = new ListNode(new SourcePosition());
				DVarNode dVarNode = new DVarNode(new SourcePosition(), 1,
						"changeMe");
				CallNode selfNode = new CallNode(new SourcePosition(),
						dVarNode, "self", listNode);
				if (lastArrayShape.getLeftShape() != null
						&& lastArrayShape.getLeftShape().equals(
								leftShapeList.get(0))) {
					tmpNode = generateComplexMapping(
							leftShapeList.get(0).getShapeNode(),
							leftShapeList.get(0).getType(),
							selfNode,
							null,
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, LEFT_ELEM_ARRAY_CHANGE,
							arrayNumber);
				} else {
					tmpNode = generateComplexMapping(
							leftShapeList.get(0).getShapeNode(),
							leftShapeList.get(0).getType(),
							selfNode,
							null,
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, null, null);
				}
				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));
				leftShapeList.remove(0);
			}

			else if (leftShapeList.size() == 0 && rightShapeList.size() > 1) {
				ListNode listNode = new ListNode(new SourcePosition());
				DVarNode dVarNode = new DVarNode(new SourcePosition(), 1,
						"changeMe");
				CallNode selfNode = new CallNode(new SourcePosition(),
						dVarNode, "self", listNode);
				if (lastArrayShape.getRightShape() != null
						&& lastArrayShape.getRightShape().equals(
								rightShapeList.get(0))) {
					tmpNode = generateComplexMapping(
							selfNode,
							null,
							rightShapeList.get(0).getShapeNode(),
							rightShapeList.get(0).getType(),
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, RIGHT_ELEM_ARRAY_CHANGE,
							arrayNumber);
				} else {
					tmpNode = generateComplexMapping(
							selfNode,
							null,
							rightShapeList.get(0).getShapeNode(),
							rightShapeList.get(0).getType(),
							Connection
									.translateSideFromIntToString(Connection.Side.DUAL),
							node, BOTH_DASGN_CHANGE, null, null);
				}
				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));
				rightShapeList.remove(0);
			}

			else if (leftShapeList.size() == 1 && rightShapeList.size() == 1) {

				final CallNode leftNode = leftShapeList.get(0).getShapeNode();
				final String leftType = leftShapeList.get(0).getType();
				final CallNode rightNode = rightShapeList.get(0).getShapeNode();
				final String rightType = rightShapeList.get(0).getType();

				if (lastArrayShape.getLeftShape() != null
						&& lastArrayShape.getLeftShape().equals(
								leftShapeList.get(0))) {
					tmpNode = generateSimpleMapping(leftNode, leftType,
							rightNode, rightType, side, node,
							LEFT_ELEM_ARRAY_CHANGE, arrayNumber);
				} else if (lastArrayShape.getRightShape() != null
						&& lastArrayShape.getRightShape().equals(
								rightShapeList.get(0))) {
					tmpNode = generateSimpleMapping(leftNode, leftType,
							rightNode, rightType, side, node,
							RIGHT_ELEM_ARRAY_CHANGE, arrayNumber);
				} else {
					tmpNode = generateSimpleMapping(leftNode, leftType,
							rightNode, rightType, side, node, null, null);
				}
				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));
				leftShapeList.clear();
				rightShapeList.clear();
			}

			else if (leftShapeList.size() == 1 && rightShapeList.size() == 0) {
				ListNode listNode = new ListNode(new SourcePosition());
				DVarNode dVarNode = new DVarNode(new SourcePosition(), 0,
						"changeMe");
				CallNode selfNode = new CallNode(new SourcePosition(),
						dVarNode, "self", listNode);
				int index = leftShapeList.size() - 1;
				if (tmpNode == null) {
					tmpNode = node;
				}

				final CallNode leftNode = leftShapeList.get(index)
						.getShapeNode();
				final String leftType = leftShapeList.get(index).getType();

				if (lastArrayShape.getLeftShape() != null
						&& lastArrayShape.getLeftShape().equals(
								leftShapeList.get(index))) {
					tmpNode = generateSimpleMapping(leftNode, leftType,
							selfNode, null, side, tmpNode,
							LEFT_ELEM_ARRAY_CHANGE, arrayNumber);
					outNode = tmpNode;
				} else {
					tmpNode = generateSimpleMapping(leftNode, leftType,
							selfNode, null, side, tmpNode, null, null);
					outNode = tmpNode;
				}
				leftShapeList.clear();
				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));
			}

			else if (leftShapeList.size() == 0 && rightShapeList.size() == 1) {
				ListNode listNode = new ListNode(new SourcePosition());
				DVarNode dVarNode = new DVarNode(new SourcePosition(), 1,
						"changeMe");
				CallNode selfNode = new CallNode(new SourcePosition(),
						dVarNode, "self", listNode);
				int index = rightShapeList.size() - 1;
				if (tmpNode == null) {
					tmpNode = node;
				}

				final CallNode rightNode = rightShapeList.get(index)
						.getShapeNode();
				final String rightType = rightShapeList.get(index).getType();

				if (lastArrayShape.getRightShape() != null
						&& lastArrayShape.getRightShape().equals(
								rightShapeList.get(index))) {
					tmpNode = generateSimpleMapping(selfNode, null, rightNode,
							rightType, side, tmpNode, RIGHT_ELEM_ARRAY_CHANGE,
							arrayNumber);
					outNode = tmpNode;
				} else {
					tmpNode = generateSimpleMapping(selfNode, null, rightNode,
							rightType, side, tmpNode, null, null);
					outNode = tmpNode;
				}
				rightShapeList.clear();
				NewlineNode newLineNode;
				if ((newLineNode = generateComment(comment)) != null) {
					getBlockNode(node).add(newLineNode);
				}
				getBlockNode(node).add((new NewlineNode(tmpNode.getPositionIncludingComments(), tmpNode)));

			}
			node = tmpNode;
		}
		return outNode;

	}

	/**
	 * Removes mapping from root tree node TODO
	 * 
	 * @param leftShape
	 * @param rightShape
	 * @param side
	 * @param comment
	 * @throws Exception
	 */
	public void removeMapping(NewlineNode nodeToRemove) {
		Node parentNode = getParentNode(nodeToRemove, rootNode);
		if (parentNode instanceof BlockNode) {
			BlockNode blockNode = (BlockNode) parentNode;
			NewlineNode parentMapNode = getParentMappingNode(nodeToRemove);;
			
			NewlineNode commentNode = null;
			if ((commentNode = getChildBefore(blockNode, nodeToRemove)) != null) {
				if (commentNode.getNextNode() != null
						&& commentNode.getNextNode() instanceof XStrNode) {
					parentNode.childNodes().remove(commentNode);
				}
			}
			
			parentNode.childNodes().remove(nodeToRemove);

			if (parentNode.childNodes().size() == 0){
				removeMapping(parentMapNode);
			}
		} else if (parentNode instanceof NewlineNode){
			removeMapping((NewlineNode) parentNode);
		}
		
		
//		List<Integer> path;
//		if (arrayNumber != null) {
//			path = findMappingPath(leftShape, rightShape, arrayNumber);
//		} else {
//			path = findMappingPath(leftShape, rightShape);
//		}
//		NewlineNode node = findRootMappingNode(rootNode);
//		List<NewlineNode> mappingNodesStack = new ArrayList<NewlineNode>();
//		mappingNodesStack.add(node);
//		for (int i : path) {
//			node = (NewlineNode) getBlockNode(node).get(i);
//			mappingNodesStack.add(node);
//		}
//
//		List<Shape> leftShapeList = leftShape.getShapeStack();
//		List<Shape> rightShapeList = rightShape.getShapeStack();
//		if (leftShapeList.size() != rightShapeList.size()) {
//			int selfSide = leftShapeList.size() - rightShapeList.size();
//			if (selfSide == 1 && rightShapeList.size() == path.size()) {
//				for (Node child : getBlockNode(
//						mappingNodesStack.get(mappingNodesStack.size() - 1))
//						.childNodes()) {
//					if (child instanceof NewlineNode) {
//						if (mappingExists(leftShape.getName(), "self",
//								(NewlineNode) child)) {
//							mappingNodesStack.add((NewlineNode) child);
//						}
//					}
//				}
//			} else if (selfSide == -1 && leftShapeList.size() == path.size()) {
//				for (Node child : getBlockNode(
//						mappingNodesStack.get(mappingNodesStack.size() - 1))
//						.childNodes()) {
//					if (child instanceof NewlineNode) {
//						if (mappingExists("self", rightShape.getName(),
//								(NewlineNode) child)) {
//							mappingNodesStack.add((NewlineNode) child);
//						}
//					}
//				}
//			}
//
//		}
//
//		Collections.reverse(mappingNodesStack);
//		BlockNode parentBlockNode = null;
//		for (int i = 0; i + 1 < mappingNodesStack.size(); i++) {
//			NewlineNode parent = mappingNodesStack.get(i + 1);
//			NewlineNode child = mappingNodesStack.get(i);
//			parentBlockNode = getBlockNode(mappingNodesStack.get(i + 1));
//			NewlineNode commentNode = null;
//			if ((commentNode = getChildBefore(parent, child)) != null) {
//				if (commentNode.getNextNode() != null
//						&& commentNode.getNextNode() instanceof XStrNode) {
//					parentBlockNode.childNodes().remove(commentNode);
//				}
//			}
//			parentBlockNode.childNodes().remove(mappingNodesStack.get(i));
//			if (parentBlockNode.childNodes().size() != 0) {
//				break;
//			}
//		}
	}

	/**
	 * Returns child of the parent with index 1 before that what is entered.
	 * This is usable for getting comment tree before mapping tree.
	 * 
	 * @param parent
	 * @param childAfter
	 * @return
	 */
	private NewlineNode getChildBefore(NewlineNode parent,
			NewlineNode childAfter) {
		BlockNode parentBlockNode = getBlockNode(parent);
		return getChildBefore(parentBlockNode, childAfter);
	}

	private NewlineNode getChildBefore(BlockNode parentBlockNode,
			NewlineNode childAfter) {
		for (int i = 0; i < parentBlockNode.childNodes().size(); i++) {
			if (parentBlockNode.childNodes().get(i).equals(childAfter)) {
				if (i > 0) {
					return (NewlineNode) parentBlockNode.childNodes()
							.get(i - 1);
				}
			}
		}
		return null;
	}

	/**
	 * Corrects nodes (inserts BlockNodes between Itarate and Newline if not
	 * exists. This usually happened when there is only one Newline after
	 * Iterate
	 * 
	 * @param node
	 * @return
	 */
	public static Node correctNodeIterationBlocks(Node node) {
		if (node instanceof IterNode) {
			if (((IterNode) node).getBodyNode() instanceof NewlineNode) {
				BlockNode blockNode = new BlockNode(new SourcePosition());
				blockNode.add(((IterNode) node).getBodyNode());
				((IterNode) node).setBodyNode(blockNode);
			}
		}
		if (node.childNodes().size() > 0) {
			for (int i = 0; i < node.childNodes().size(); i++) {
				node.childNodes().set(i,
						correctNodeIterationBlocks(node.childNodes().get(i)));
			}
		}
		return node;
	}

	/**
	 * Finds longest path of mappings already created
	 * 
	 * @param leftShape
	 * @param rightShape
	 * @return
	 */
	public List<Integer> findMappingPath(Shape leftShape, Shape rightShape) {
		return findMappingPath(leftShape, rightShape,
				findRootBlockNode(rootNode), 0, 0, null, null, -1);
	}

	private Pair findLastArrayShape(List<Shape> leftShapeList,
			List<Shape> rightShapeList) {
		Shape lastLeftArrayShape = null;
		Shape lastRightArrayShape = null;
		int leftCounter = -1;
		int righCounter = -1;
		for (Shape tmpShape : leftShapeList) {
			if (tmpShape.isArrayType()) {
				lastLeftArrayShape = tmpShape;
				leftCounter++;
			}
		}
		for (Shape tmpShape : rightShapeList) {
			if (tmpShape.isArrayType()) {
				lastRightArrayShape = tmpShape;
				righCounter++;
			}
		}
		if (leftCounter > righCounter) {
			lastRightArrayShape = null;
		} else if (leftCounter < righCounter) {
			lastLeftArrayShape = null;
		} else {
			lastLeftArrayShape = null;
			lastRightArrayShape = null;
		}
		return new Pair(lastLeftArrayShape, lastRightArrayShape);
	}

	/**
	 * Finds longest path of mappings already created
	 * 
	 * @param leftShape
	 * @param rightShape
	 * @return
	 */
	public List<Integer> findMappingPath(Shape leftShape, Shape rightShape,
			int arrayNumber) {
		List<Shape> leftShapeList = leftShape.getShapeStack();
		List<Shape> rightShapeList = rightShape.getShapeStack();
		Collections.reverse(leftShapeList);
		Collections.reverse(rightShapeList);
		Pair pair = findLastArrayShape(leftShapeList, rightShapeList);
		return findMappingPath(leftShape, rightShape,
				findRootBlockNode(rootNode), 0, 0, pair.getLeftShape(), pair
						.getRightShape(), arrayNumber);
	}

	private List<Integer> findMappingPath(Shape leftShape, Shape rightShape,
			BlockNode rootNode, int leftLevel, int rightLevel,
			Shape lastLeftArrayShape, Shape lastRightArrayShape, int arrayNumber) {
		List<Integer> route = new ArrayList<Integer>();
		List<Shape> leftShapeList = leftShape.getShapeStack();
		List<Shape> rightShapeList = rightShape.getShapeStack();
		Collections.reverse(leftShapeList);
		Collections.reverse(rightShapeList);
		int leftElements = leftShapeList.size();
		int rightElements = rightShapeList.size();

		if (leftLevel < leftElements && rightLevel < rightElements) {
			List<List<Integer>> routeArray = new ArrayList<List<Integer>>();
			for (int i = 0; i < rootNode.childNodes().size(); i++) {

				if (mappingExists(leftShapeList.get(leftLevel).getName(),
						"self", (NewlineNode) rootNode.childNodes().get(i))) {
					if ((lastLeftArrayShape == null)
							|| (lastLeftArrayShape == leftShapeList
									.get(leftLevel) && checkIfMappingIsVarToArray(
									(NewlineNode) rootNode.childNodes().get(i),
									arrayNumber))) {
						List<Integer> tmpList = new ArrayList<Integer>();
						tmpList.add(i);
						if (getNextChildBlockNode((NewlineNode) rootNode
								.childNodes().get(i)) != null) {
							tmpList
									.addAll(findMappingPath(
											leftShape,
											rightShape,
											getNextChildBlockNode((NewlineNode) rootNode
													.childNodes().get(i)),
											leftLevel + 1, rightLevel,
											lastLeftArrayShape,
											lastRightArrayShape, arrayNumber));

						}
						routeArray.add(tmpList);
					}
				} else if (mappingExists("self", rightShapeList.get(rightLevel)
						.getName(), (NewlineNode) rootNode.childNodes().get(i))) {
					if ((lastRightArrayShape == null)
							|| (lastRightArrayShape == rightShapeList
									.get(rightLevel) && checkIfMappingIsVarToArray(
									(NewlineNode) rootNode.childNodes().get(i),
									arrayNumber))) {
						List<Integer> tmpList = new ArrayList<Integer>();
						tmpList.add(i);
						if (getNextChildBlockNode((NewlineNode) rootNode
								.childNodes().get(i)) != null) {
							tmpList
									.addAll(findMappingPath(
											leftShape,
											rightShape,
											getNextChildBlockNode((NewlineNode) rootNode
													.childNodes().get(i)),
											leftLevel, rightLevel + 1,
											lastLeftArrayShape,
											lastRightArrayShape, arrayNumber));

						}
						routeArray.add(tmpList);
					}
				} else if (mappingExists(
						leftShapeList.get(leftLevel).getName(), rightShapeList
								.get(rightLevel).getName(),
						(NewlineNode) rootNode.childNodes().get(i))) {
					if ((lastLeftArrayShape == null && lastRightArrayShape == null)
							|| (lastLeftArrayShape != null
									&& lastLeftArrayShape == rightShapeList
											.get(leftLevel) && checkIfMappingIsVarToArray(
									(NewlineNode) rootNode.childNodes().get(i),
									arrayNumber))
							|| (lastRightArrayShape != null
									&& lastRightArrayShape == rightShapeList
											.get(rightLevel) && checkIfMappingIsVarToArray(
									(NewlineNode) rootNode.childNodes().get(i),
									arrayNumber))) {
						List<Integer> tmpList = new ArrayList<Integer>();
						tmpList.add(i);
						if (getNextChildBlockNode((NewlineNode) rootNode
								.childNodes().get(i)) != null) {
							if (leftElements == 1 && rightElements > 1) {
								tmpList
										.addAll(findMappingPath(
												leftShape,
												rightShape,
												getNextChildBlockNode((NewlineNode) rootNode
														.childNodes().get(i)),
												leftLevel, rightLevel + 1,
												lastLeftArrayShape,
												lastRightArrayShape,
												arrayNumber));
							} else if (leftElements > 1 && rightElements == 1) {
								tmpList
										.addAll(findMappingPath(
												leftShape,
												rightShape,
												getNextChildBlockNode((NewlineNode) rootNode
														.childNodes().get(i)),
												leftLevel + 1, rightLevel,
												lastLeftArrayShape,
												lastRightArrayShape,
												arrayNumber));
							} else {
								tmpList
										.addAll(findMappingPath(
												leftShape,
												rightShape,
												getNextChildBlockNode((NewlineNode) rootNode
														.childNodes().get(i)),
												leftLevel + 1, rightLevel + 1,
												lastLeftArrayShape,
												lastRightArrayShape,
												arrayNumber));
							}

						}
						routeArray.add(tmpList);
					}
				}
			}

			List<Integer> tmpList = new ArrayList<Integer>();
			for (List<Integer> list : routeArray) {
				if (tmpList.size() < list.size())
					tmpList = list;
			}
			route.addAll(tmpList);
		}

		return route;
	}

	// private List<Integer> findMappingPath(Shape leftShape, Shape rightShape,
	// BlockNode rootNode, int level) {
	// List<Integer> route = new ArrayList<Integer>();
	// List<Shape> leftShapeList = leftShape.getShapeStack();
	// List<Shape> rightShapeList = rightShape.getShapeStack();
	// Collections.reverse(leftShapeList);
	// Collections.reverse(rightShapeList);
	// int elements;
	// if (leftShapeList.size() > rightShapeList.size())
	// elements = rightShapeList.size();
	// else
	// elements = leftShapeList.size();
	//
	// if (level < elements) {
	// List<List<Integer>> routeArray = new ArrayList<List<Integer>>();
	// for (int i = 0; i < rootNode.childNodes().size(); i++) {
	//				
	// if (mappingExists(leftShapeList.get(level), rightShapeList
	// .get(level), (NewlineNode) rootNode.childNodes().get(i))) {
	// List<Integer> tmpList = new ArrayList<Integer>();
	// tmpList.add(i);
	// if (getNextChildBlockNode((NewlineNode) rootNode
	// .childNodes().get(i)) != null) {
	// tmpList.addAll(findMappingPath(leftShape, rightShape,
	// getNextChildBlockNode((NewlineNode) rootNode
	// .childNodes().get(i)), level + 1));
	//
	// }
	// routeArray.add(tmpList);
	// }
	// }
	//
	// List<Integer> tmpList = new ArrayList<Integer>();
	// for (List<Integer> list : routeArray) {
	// if (tmpList.size() < list.size())
	// tmpList = list;
	// }
	// route.addAll(tmpList);
	// }
	//
	// return route;
	// }

	/**
	 * Returns next BlockNode of inserted complex mapping Node
	 * 
	 * @param node
	 * @return
	 */
	private BlockNode getNextChildBlockNode(NewlineNode node) {
		if (node.getNextNode() instanceof FCallNode
				&& ((FCallNode) node.getNextNode()).getIterNode() instanceof IterNode) {
			return (BlockNode) ((IterNode) ((FCallNode) node.getNextNode())
					.getIterNode()).getBodyNode();

		} else
			return null;
	}

	/**
	 * Check if mapping exists in inserted Node
	 * 
	 * @param leftShape
	 * @param rightShape
	 * @param newlineNode
	 * @return
	 */
	private boolean mappingExists(String leftShapeName, String rightShapeName,
			NewlineNode newlineNode) {
		if (newlineNode.getNextNode() instanceof FCallNode) {
			if (((FCallNode) newlineNode.getNextNode()).getName().equals("map")) {
				CallNode callnode = (CallNode) newlineNode.getNextNode()
						.childNodes().get(0).childNodes().get(0);
				if (callnode.childNodes().get(0) instanceof StrNode) {
					StrNode leftNode = ((StrNode) callnode.childNodes().get(0));
					CallNode rightNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(1).childNodes().get(0));
					if (leftNode.getValue().equals(leftShapeName)
							&& rightNode.getName().equals(rightShapeName)) {
						return true;
					}

				} else if (callnode.childNodes().get(1).childNodes().get(0) instanceof StrNode) {
					CallNode leftNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(0));
					StrNode rightNode = ((StrNode) callnode.childNodes().get(1)
							.childNodes().get(0));
					if (leftNode.getName().equals(leftShapeName)
							&& rightNode.getValue().equals(rightShapeName)) {
						return true;
					}
				} else if (callnode.childNodes().get(0) instanceof VCallNode) {
					VCallNode leftNode = ((VCallNode) callnode.childNodes()
							.get(0));
					CallNode rightNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(1).childNodes().get(0));
					if (leftNode.getName().equals("func")
							&& rightNode.getName().equals(rightShapeName)) {
						return true;
					}
				} else if (callnode.childNodes().get(1).childNodes().get(0) instanceof VCallNode) {
					CallNode leftNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(0));
					VCallNode rightNode = ((VCallNode) callnode.childNodes()
							.get(1).childNodes().get(0));
					if (leftNode.getName().equals(leftShapeName)
							&& rightNode.getName().equals("func")) {
						return true;
					}
				} else {
					CallNode leftNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(0));
					CallNode rightNode = ModelGenerator
							.findLastCallNodeInTree(callnode.childNodes()
									.get(1).childNodes().get(0));
					if (leftNode.getName().equals(leftShapeName)
							&& rightNode.getName().equals(rightShapeName)) {
						return true;
					}
				}

			}
		}
		return false;
	}

	private StaticScope getNodeScope(NewlineNode node) {
		return ((IterNode) ((FCallNode) node.childNodes().get(0)).getIterNode())
				.getScope();
	}

	/**
	 * Generates complex mapping tree node (complex means if mapping is a parent
	 * for other mappings)
	 * 
	 * @param leftSide
	 * @param rightSide
	 * @param side
	 * @param parentMapping
	 * @param whichDAsagnChange
	 *            sets which side of the mapping should have generated new
	 *            assign prefixes
	 * @return
	 */
	private NewlineNode generateComplexMapping(final CallNode leftSideIn,
			final String leftType, final CallNode rightSideIn,
			final String rightType, final String side,
			final NewlineNode parentMapping, final int whichDAsagnChange,
			final Integer arrayElementSide, final Integer arrayNumber) {
		CallNode[] nodes = addArrayElementDeclaration(leftSideIn, rightSideIn,
				arrayElementSide, arrayNumber);

		CallNode leftSide = nodes[0];
		CallNode rightSide = nodes[1];

		addTypesToNodes(leftSide, leftType, rightSide, rightType);

		String leftPrefix = generateRandomIdent();
		String rightPrefix = generateRandomIdent();
		String[] s = findDVars(parentMapping);

		if (whichDAsagnChange == LEFT_DASGN_CHANGE) {
			rightPrefix = s[1];
			rightSide = cloneCallNode(rightSide);

		} else if (whichDAsagnChange == RIGHT_DASGN_CHANGE) {
			leftPrefix = s[0];
			leftSide = cloneCallNode(leftSide);
		} else if (whichDAsagnChange == BOTH_DASGN_CHANGE){
			rightSide = cloneCallNode(rightSide);
			leftSide = cloneCallNode(leftSide);
		}
		DAsgnNode leftAsgnNode = new DAsgnNode(new SourcePosition(),
				leftPrefix, 0, NilImplicitNode.NIL);
		DAsgnNode rightAsgnNode = new DAsgnNode(new SourcePosition(),
				rightPrefix, 1, NilImplicitNode.NIL);
		ArrayNode dasgnArrayNode = new ArrayNode(new SourcePosition());
		dasgnArrayNode.add(leftAsgnNode);
		dasgnArrayNode.add(rightAsgnNode);
		MultipleAsgnNode multipleAsgnNode = new MultipleAsgnNode(
				new SourcePosition(), dasgnArrayNode, null);
		StaticScope scope = new BlockStaticScope(getNodeScope(parentMapping),
				new String[] { leftPrefix.intern(), rightPrefix.intern() });
		BlockNode blockNode = new BlockNode(new SourcePosition());
		IterNode iterNode = new IterNode(new SourcePosition(),
				multipleAsgnNode, scope, blockNode);

		ArrayNode argsNode = new ArrayNode(new SourcePosition(), rightSide);
		CallNode callNode = new CallNode(new SourcePosition(), leftSide, side,
				argsNode);
		ArrayNode arrayNode = new ArrayNode(new SourcePosition(), callNode);
		FCallNode fcallMapNode = new FCallNode(new SourcePosition(), "map",
				arrayNode);
		fcallMapNode.setIterNode(iterNode);
		NewlineNode newlineNode = new NewlineNode(new SourcePosition(),
				fcallMapNode);

		changeMappingDVars(newlineNode, s[0], s[1]);
		return newlineNode;
	}

	private CallNode cloneCallNode(CallNode cnode) {
		Node receiverNode = cnode.getReceiverNode();
		if (receiverNode instanceof CallNode) {
			receiverNode = cloneCallNode( (CallNode) receiverNode);
		}
		if (receiverNode instanceof DVarNode) {
			DVarNode dvarnd = (DVarNode) receiverNode;
			receiverNode = new DVarNode(dvarnd.getPositionIncludingComments(),0,dvarnd.getName());
		}
		Node argsNode = cnode.getArgsNode();
		if (argsNode instanceof CallNode) {
			argsNode = cloneCallNode( (CallNode) argsNode);
		}
		if (argsNode instanceof DVarNode) {
			DVarNode dvarnd = (DVarNode) argsNode;
			argsNode = new DVarNode(dvarnd.getPositionIncludingComments(),0,dvarnd.getName());
		}

		CallNode callNode = new CallNode(cnode.getPositionIncludingComments(), receiverNode, cnode.getName(), argsNode);
		return callNode;
	}

	/**
	 * Change mapping Dvars eg. "<b>a</b>.place"
	 * 
	 * @param node
	 * @param leftDVarName
	 * @param rightDVarName
	 */
	private void changeMappingDVars(NewlineNode node, String leftDVarName,
			String rightDVarName) {

		changeDVar(getMappingCallNode(node).childNodes().get(0), leftDVarName);
		changeDVar(getMappingCallNode(node).childNodes().get(1).childNodes()
				.get(0), rightDVarName);

	}

	private void changeDVar(Node node, String name) {
		if (node instanceof DVarNode) {
			((DVarNode) node).setName(name);
		} else if (node.childNodes().size() > 0) {
			changeDVar(node.childNodes().get(0), name);
		}

	}

	private CallNode getMappingCallNode(NewlineNode node) {
		return (CallNode) node.childNodes().get(0).childNodes().get(0)
				.childNodes().get(0);
	}

	public String generateRandomIdent() {
		usedCharIndex++;

		String myRandom = "";
		int firstDigitIndex = usedCharIndex / 625;
		int secondDigitIndex = (usedCharIndex % 625) / 25;
		int identIndex = usedCharIndex % 25;
		if (firstDigitIndex > 0) {
			myRandom = "" + charArray[firstDigitIndex - 1] + ""
					+ charArray[secondDigitIndex - 1] + ""
					+ charArray[identIndex - 1];
		} else if (secondDigitIndex > 0) {
			myRandom = "" + charArray[secondDigitIndex - 1] + ""
					+ charArray[identIndex - 1];
		} else {
			myRandom = "" + charArray[identIndex - 1];
		}

		if (usedIdent.contains(myRandom)) {
			myRandom = generateRandomIdent();
		}
		usedIdent.add(myRandom);
		return myRandom;
	}

	/**
	 * Generate root tree if mapping is generated from XSD schemas
	 * 
	 * @param leftElement
	 * @param rightElement
	 * @param requirements
	 */
	public void generateRootNode(String leftElement, String rightElement,
			List<String> requirements) {
		BlockStaticScope rootStaticScope = new BlockStaticScope(null);
		// creating Node
		BlockNode blockNode = new BlockNode(new SourcePosition());
		this.rootNode = new RootNode(new SourcePosition(), rootStaticScope,
				blockNode);
		new BlockNode(new SourcePosition());

		if (requirements == null) {
			requirements = new ArrayList<String>();
		}
		requirements.add(0, MAPPUM_STR);

		// adding requirements
		for (String req : requirements) {
			StrNode stringNode = new StrNode(new SourcePosition(), req);
			ArrayNode arrayNode = new ArrayNode(new SourcePosition(),
					stringNode);
			FCallNode requireNode = new FCallNode(new SourcePosition(),
					REQUIRE_STR, arrayNode);
			NewlineNode newlineNode = new NewlineNode(new SourcePosition(),
					requireNode);
			blockNode.add(newlineNode);
		}
		BlockNode rootBlockNode = new BlockNode(new SourcePosition());

		ConstNode constNode = new ConstNode(new SourcePosition(), "Mappum");
		ListNode listNode = new ListNode(new SourcePosition());
		BlockStaticScope blockStaticScope = new BlockStaticScope(
				rootStaticScope);
		IterNode rootIterNode = new IterNode(new SourcePosition(), null,
				blockStaticScope, rootBlockNode);
		CallNode rootCallNode = new CallNode(new SourcePosition(), constNode,
				CATALOGUE_STR, listNode, rootIterNode);
		NewlineNode newlineCallNode = new NewlineNode(new SourcePosition(),
				rootCallNode);
		blockNode.add(newlineCallNode);
		// ArrayNode z mappingiem
		String leftPrefix = generateRandomIdent();
		String rightPrefix = generateRandomIdent();
		NilImplicitNode leftNiNode = new NilImplicitNode();
		NilImplicitNode rightNiNode = new NilImplicitNode();
		DAsgnNode leftAsgnNode = new DAsgnNode(new SourcePosition(),
				leftPrefix, 0, leftNiNode);
		DAsgnNode rightAsgnNode = new DAsgnNode(new SourcePosition(),
				rightPrefix, 1, rightNiNode);
		ArrayNode dasgnArrayNode = new ArrayNode(new SourcePosition());
		dasgnArrayNode.add(leftAsgnNode);
		dasgnArrayNode.add(rightAsgnNode);
		MultipleAsgnNode multipleAsgnNode = new MultipleAsgnNode(
				new SourcePosition(), dasgnArrayNode, null);
		StaticScope scope = new BlockStaticScope(blockStaticScope,
				new String[] { leftPrefix, rightPrefix });
		// BlockNode mappingBlockNode = new BlockNode(new SourcePosition());
		// IterNode iterNode = new IterNode(new SourcePosition(),
		// multipleAsgnNode, scope, mappingBlockNode);
		IterNode iterNode = new IterNode(new SourcePosition(),
				multipleAsgnNode, scope, null);

		ConstNode leftConstNode = new ConstNode(new SourcePosition(),
				leftElement);
		ConstNode rightConstNode = new ConstNode(new SourcePosition(),
				rightElement);

		ArrayNode arrayNode = new ArrayNode(new SourcePosition());
		arrayNode.add(leftConstNode);
		arrayNode.add(rightConstNode);
		FCallNode fcallMapNode = new FCallNode(new SourcePosition(), "map",
				arrayNode);
		fcallMapNode.setIterNode(iterNode);
		NewlineNode newlineNode = new NewlineNode(new SourcePosition(),
				fcallMapNode);
		rootBlockNode.add(newlineNode);
	}

	public static void generateRootBlockNode(Node node) {
		boolean iterate = true;
		for (Node child : node.childNodes()) {
			if (child instanceof FCallNode) {
				if (((FCallNode) child).getName() == "map") {
					iterate = false;
					if (((FCallNode) child).getIterNode() != null) {
						if (((FCallNode) child).getIterNode() instanceof IterNode)
							if (((FCallNode) child).getIterNode().childNodes()
									.size() < 2) {
								IterNode iterNode = (IterNode) ((FCallNode) child)
										.getIterNode();
								iterNode.setBodyNode(new BlockNode(
										new SourcePosition()));
							}
					}

				}
			}
			if (iterate == true)
				generateRootBlockNode(child);
		}
	}

	/**
	 * Checks the name of the left side object of mapping
	 * 
	 * @param callnode
	 * @return
	 */
	public static String checkLeftSideMappingName(CallNode callnode) {
		CallNode leftNode = ModelGenerator.findLastCallNodeInTree(callnode
				.childNodes().get(0));
		return leftNode.getName();
	}

	/**
	 * Checks the name of the right side object of mapping
	 * 
	 * @param callnode
	 * @return
	 */
	public static String checkRightSideMappingName(CallNode callnode) {
		CallNode rightNode = ModelGenerator.findLastCallNodeInTree(callnode
				.childNodes().get(1).childNodes().get(0));
		return rightNode.getName();
	}
}
