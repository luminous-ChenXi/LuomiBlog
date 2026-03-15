import { defineCollection, z } from 'astro:content';

const blogCollection = defineCollection({
  type: 'content',
  schema: z.object({
    title: z.string(),
    description: z.string(),
    pubDate: z.coerce.date(),
    author: z.string().default('辰汐'),
    tags: z.array(z.string()).default([]),
    category: z.string().default('未分类'),
    cover: z.string().optional(),
    views: z.number().default(0),
    comments: z.number().default(0),
    pinned: z.boolean().default(false),
  }),
});

export const collections = {
  'blog': blogCollection,
};
