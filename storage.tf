resource "aws_s3_bucket" "bucket1" {
  bucket = var.loki_bucket 
  force_destroy = true

  tags = {
    Name        = var.username
    Environment = "Dev"
    email = var.user_email

  }
}

resource "aws_s3_bucket" "bucket2" {
  force_destroy = true
  bucket = var.mimir_bucket 

  tags = {
    Name        = var.username
    Environment = "Dev"
    email = var.user_email
    
  }
}

#policy for the service account that's been created to read and write the s3 buckets 
data "aws_iam_policy_document" "s3_policy" {
  statement {
    sid = "1"
    effect = "Allow"
    actions = [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket"
    ]

    resources = [
                "arn:aws:s3:::${var.mimir_bucket}",
                "arn:aws:s3:::${var.mimir_bucket}/*",
                "arn:aws:s3:::${var.loki_bucket}",
                "arn:aws:s3:::${var.loki_bucket}/*"
                
    ]
  }
}
resource "aws_iam_policy" "s3_policy_json" {
  name   = "access_loki_and_mimir"
  path   = "/"
  policy = data.aws_iam_policy_document.s3_policy.json
}


resource "aws_iam_role_policy_attachment" "db-node-group-attach-s3-policy" {
  role       = module.eks.eks_managed_node_groups.one.iam_role_name
  policy_arn = aws_iam_policy.s3_policy_json.arn
  #depends_on = [module.eks.eks_managed_node_groups.one.name]
}


resource "aws_iam_role_policy_attachment" "platform-node-group-attach-s3-policy" {
  role       = module.eks.eks_managed_node_groups.two.iam_role_name
  policy_arn = aws_iam_policy.s3_policy_json.arn
  #depends_on = [module.eks.eks_managed_node_groups.one.name]
}
